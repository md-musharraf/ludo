package com.example.ludo.engine

import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.core.logging.AppLogger
import com.example.ludo.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Ludo rules engine and turn driver.
 *
 * Thread confinement: every public method must be called from the thread backing [scope]
 * (the main thread when [scope] is `viewModelScope`). All engine coroutines run on that same
 * scope, so check-then-update sequences are race free without locks.
 *
 * Human entry points ([rollDice], [selectToken]) are rejected while an AI player is on turn,
 * so the UI can never act on behalf of a bot.
 */
class GameEngine(
    private val scope: CoroutineScope,
    private val random: Random = Random.Default
) {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val moveValidator = MoveValidator()
    private var aiPlayer: AIPlayer? = null
    private var gameJob: Job = SupervisorJob()
    private var hopSeq = 0L
    private var effectSeq = 0L

    fun resetGame(playerCount: Int, isVsAI: Boolean, aiDifficulty: AiDifficulty = AiDifficulty.HARD) {
        gameJob.cancel()
        gameJob = SupervisorJob(scope.coroutineContext[Job])
        AppLogger.i(TAG) { "Resetting game: players=$playerCount, isVsAI=$isVsAI, difficulty=$aiDifficulty" }

        val selectedColors = when (playerCount) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
            3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
            else -> PlayerColor.entries
        }

        val players = selectedColors.mapIndexed { i, color ->
            val isAI = isVsAI && i > 0
            val colorOrdinal = color.ordinal
            Player(
                id = colorOrdinal,
                color = color,
                name = if (isAI) "Bot ${color.displayName()}" else "Player ${i + 1}",
                isAI = isAI,
                tokens = List(4) { tokenId ->
                    Token(id = tokenId, playerId = colorOrdinal, boardPosition = BoardConfig.homeSpot(colorOrdinal, tokenId))
                }
            )
        }

        _state.value = GameState(
            players = players,
            moveMessage = rollPrompt(players.first())
        )
        aiPlayer = if (isVsAI) AIPlayer(aiDifficulty, random) else null
        continueTurn()
    }

    /** Replaces the whole game state (tests and save/restore), cancelling any in-flight turn. */
    internal fun loadState(state: GameState, aiDifficulty: AiDifficulty = AiDifficulty.HARD) {
        gameJob.cancel()
        gameJob = SupervisorJob(scope.coroutineContext[Job])
        _state.value = state
        aiPlayer = if (state.players.any { it.isAI }) AIPlayer(aiDifficulty, random) else null
        continueTurn()
    }

    /** Human roll request. Ignored when it is not a human's turn or a roll is not allowed now. */
    fun rollDice() {
        if (_state.value.currentPlayer?.isAI != false) return
        val roller = beginRoll() ?: return
        launch { finishRoll(roller) }
    }

    /** Human token selection. Ignored for AI turns and for tokens that are not valid moves. */
    fun selectToken(tokenId: Int) {
        if (_state.value.currentPlayer?.isAI != false) return
        val plan = beginMove(tokenId) ?: return
        launch { runMove(plan) }
    }

    // region Rolling

    /** Synchronously claims the roll so double taps cannot start two rolls. */
    private fun beginRoll(): Player? {
        val s = _state.value
        if (s.gamePhase != GamePhase.WAITING_FOR_ROLL || s.isGameOver || s.isDiceRollingForPlayer != null) return null
        val player = s.currentPlayer ?: return null
        AppLogger.d(TAG) { "${player.name} initiated dice roll" }
        _state.update { it.copy(isDiceRollingForPlayer = player.id, moveMessage = "${player.name} is rolling...") }
        SoundEffectManager.playDiceRoll()
        return player
    }

    private suspend fun finishRoll(player: Player) {
        delay(ROLL_MS)
        val roll = random.nextInt(1, 7)
        AppLogger.i(TAG) { "${player.name} rolled: $roll" }
        if (roll == 6) SoundEffectManager.playSixRolled()

        val sixes = if (roll == 6) _state.value.consecutiveSixes + 1 else 0
        val rolled = _state.value.copy(
            diceResult = DiceResult(roll),
            isDiceRollingForPlayer = null,
            consecutiveSixes = sixes,
            validMoves = emptyList()
        )

        if (sixes >= MAX_CONSECUTIVE_SIXES) {
            AppLogger.w(TAG) { "${player.name} rolled three consecutive sixes! Turn forfeited." }
            _state.value = rolled.copy(moveMessage = "Three 6s! Turn forfeited ⚠️")
            delay(TURN_PAUSE_MS)
            advanceTurn()
            return
        }

        val moves = moveValidator.getValidMoves(rolled)
        if (moves.isEmpty()) {
            _state.value = rolled.copy(moveMessage = "${player.name} rolled a $roll. No valid moves!")
            delay(TURN_PAUSE_MS)
            advanceTurn()
            return
        }

        val bonusText = if (roll == 6) " (Bonus roll on 6!)" else ""
        val autoMove = if (player.isAI) aiPlayer?.chooseMove(rolled, moves) else obviousMove(player, moves)
        val message = when {
            player.isAI -> "${player.name} rolled $roll! Thinking...$bonusText"
            autoMove != null -> "${player.name} rolled $roll! Auto-moving piece...$bonusText"
            else -> "${player.name} rolled a $roll! Tap a glowing piece$bonusText"
        }
        _state.value = rolled.copy(gamePhase = GamePhase.WAITING_FOR_MOVE, validMoves = moves, moveMessage = message)

        if (autoMove != null) {
            delay(if (player.isAI) AI_THINK_MS else AUTO_MOVE_MS)
            beginMove(autoMove)?.let { runMove(it) }
        }
    }

    /**
     * Returns a token to auto-move when the choice doesn't matter: a single option, or every
     * option is an identical token (e.g. several pieces still in base on a 6).
     */
    private fun obviousMove(player: Player, moves: List<Int>): Int? {
        val candidates = player.tokens.filter { it.id in moves }
        val first = candidates.firstOrNull() ?: return null
        return first.id.takeIf {
            candidates.all { it.state == first.state && it.positionIndex == first.positionIndex }
        }
    }

    // endregion

    // region Moving

    private class MovePlan(val playerIndex: Int, val token: Token, val roll: Int)

    /** Synchronously validates and claims the move so a token can't be moved twice. */
    private fun beginMove(tokenId: Int): MovePlan? {
        val s = _state.value
        if (s.gamePhase != GamePhase.WAITING_FOR_MOVE || s.isGameOver || tokenId !in s.validMoves) return null
        val player = s.currentPlayer ?: return null
        val token = player.tokens.firstOrNull { it.id == tokenId } ?: return null
        val roll = s.diceResult?.value ?: return null

        AppLogger.i(TAG) { "${player.name} moves token $tokenId (pos: ${token.positionIndex}) by $roll" }
        _state.update {
            it.copy(
                gamePhase = GamePhase.ANIMATING_MOVE,
                validMoves = emptyList(),
                moveMessage = "${player.name} is moving piece ${tokenId + 1}..."
            )
        }
        return MovePlan(s.currentPlayerIndex, token, roll)
    }

    private suspend fun runMove(plan: MovePlan) {
        val moved = walkToken(plan)
        resolveLanding(plan.playerIndex, moved, plan.roll)
    }

    private suspend fun walkToken(plan: MovePlan): Token {
        val playerId = _state.value.players[plan.playerIndex].id
        val path = PathMapper.getPlayerPath(playerId)
        var current = plan.token

        if (current.state == TokenState.IN_HOME) {
            SoundEffectManager.playLeaveBase()
            hop(playerId, current.id, BoardConfig.homeSpot(playerId, current.id), path[0], LEAVE_BASE_HOP_MS)
            current = current.copy(state = TokenState.ON_BOARD, positionIndex = 0, boardPosition = path[0])
            commitToken(plan.playerIndex, current)
            return current
        }

        repeat(plan.roll) {
            val next = current.positionIndex + 1
            SoundEffectManager.playTokenStep()
            hop(playerId, current.id, path[current.positionIndex], path[next], STEP_HOP_MS)
            current = current.copy(state = stateForIndex(next), positionIndex = next, boardPosition = path[next])
            commitToken(plan.playerIndex, current)
        }
        return current
    }

    private suspend fun hop(playerId: Int, tokenId: Int, from: Pair<Int, Int>, to: Pair<Int, Int>, durationMs: Int) {
        _state.update { it.copy(hop = HopMove(playerId, tokenId, from, to, durationMs, ++hopSeq)) }
        delay(durationMs.toLong())
    }

    /** Writes the token and clears the hop in one emission so the UI never draws it twice. */
    private fun commitToken(playerIndex: Int, token: Token) {
        _state.update { s ->
            s.copy(
                hop = null,
                players = s.players.mapIndexed { i, p ->
                    if (i != playerIndex) p else p.copy(tokens = p.tokens.map { if (it.id == token.id) token else it })
                }
            )
        }
    }

    private suspend fun resolveLanding(playerIndex: Int, token: Token, roll: Int) {
        val playerName = _state.value.players[playerIndex].name
        var bonusTurn = roll == 6
        var note = ""

        if (token.state == TokenState.FINISHED) {
            bonusTurn = true
            note = " 🎉 A piece reached HOME!"
            SoundEffectManager.playSixRolled()
            emitEffect(BoardEffect.Kind.FINISH, _state.value.players[playerIndex].color, token.boardPosition)
        }

        val cell = token.boardPosition
        if (token.isOnMainTrack && cell != null && !BoardConfig.isSafe(cell) && captureAt(playerIndex, cell)) {
            bonusTurn = true
            note = " 💥 Captured an opponent!"
        }

        val finishedRank = markFinishedIfDone(playerIndex)
        if (finishGameIfDecided()) return

        if (bonusTurn && finishedRank == null) {
            _state.update {
                it.copy(
                    gamePhase = GamePhase.WAITING_FOR_ROLL,
                    validMoves = emptyList(),
                    diceResult = null,
                    moveMessage = "$playerName gets a bonus turn!$note"
                )
            }
            continueTurn()
        } else {
            advanceTurn(prefix = finishedRank?.let { "🏅 $playerName finished #$it!" })
        }
    }

    /** Sends every opponent token on [cell] back to base, animating each return. */
    private suspend fun captureAt(capturerIndex: Int, cell: Pair<Int, Int>): Boolean {
        val capturer = _state.value.players[capturerIndex]
        val victims = _state.value.players.withIndex()
            .filter { (i, _) -> i != capturerIndex }
            .flatMap { (i, p) -> p.tokens.filter { it.isOnMainTrack && it.boardPosition == cell }.map { i to it } }
        if (victims.isEmpty()) return false

        SoundEffectManager.playCapture()
        emitEffect(BoardEffect.Kind.CAPTURE, capturer.color, cell)
        for ((victimIndex, victim) in victims) {
            val owner = _state.value.players[victimIndex]
            AppLogger.i(TAG) { "${capturer.name} captured ${owner.name}'s token ${victim.id} at $cell" }
            val home = BoardConfig.homeSpot(owner.id, victim.id)
            hop(owner.id, victim.id, cell, home, CAPTURE_HOP_MS)
            commitToken(victimIndex, victim.copy(state = TokenState.IN_HOME, positionIndex = -1, boardPosition = home))
        }
        return true
    }

    private fun emitEffect(kind: BoardEffect.Kind, color: PlayerColor, cell: Pair<Int, Int>?) {
        if (cell == null) return
        _state.update { it.copy(effect = BoardEffect(kind, color, cell, ++effectSeq)) }
    }

    private fun stateForIndex(index: Int): TokenState = when {
        index >= BoardConfig.GOAL_INDEX -> TokenState.FINISHED
        index >= BoardConfig.HOME_COLUMN_START -> TokenState.IN_HOME_COLUMN
        else -> TokenState.ON_BOARD
    }

    // endregion

    // region Turn flow and results

    /** Ranks the player if all their tokens are home; returns the new rank, or null. */
    private fun markFinishedIfDone(playerIndex: Int): Int? {
        val player = _state.value.players[playerIndex]
        if (player.hasFinished || player.tokens.any { it.state != TokenState.FINISHED }) return null
        val rank = _state.value.players.count { it.hasFinished } + 1
        AppLogger.i(TAG) { "${player.name} finished in place $rank" }
        _state.update { s ->
            s.copy(players = s.players.mapIndexed { i, p -> if (i == playerIndex) p.copy(hasFinished = true, rank = rank) else p })
        }
        return rank
    }

    /**
     * The match is decided when at most one player is still racing, or — in a game with humans —
     * when every human has finished (bots racing each other would just be a wait).
     */
    private fun finishGameIfDecided(): Boolean {
        val players = _state.value.players
        val racing = players.filter { !it.hasFinished }
        val humansInGame = players.any { !it.isAI }
        val decided = racing.size <= 1 || (humansInGame && racing.none { !it.isAI })
        if (!decided) return false

        // Remaining players are ranked by how far their pieces have travelled.
        var nextRank = players.count { it.hasFinished } + 1
        val remainingRanks = racing
            .sortedByDescending { p -> p.tokens.sumOf { it.positionIndex.coerceAtLeast(0) } }
            .associate { it.id to nextRank++ }
        val ranked = players.map { p -> remainingRanks[p.id]?.let { p.copy(rank = it) } ?: p }
        val winner = ranked.first { it.rank == 1 }

        AppLogger.i(TAG) { "🏆 Match won by ${winner.name} (id: ${winner.id})" }
        SoundEffectManager.playWinFanfare()
        _state.update {
            it.copy(
                players = ranked,
                gamePhase = GamePhase.GAME_OVER,
                isGameOver = true,
                winnerId = winner.id,
                validMoves = emptyList(),
                hop = null,
                moveMessage = "🏆 ${winner.name} WINS THE MATCH!"
            )
        }
        return true
    }

    private fun advanceTurn(prefix: String? = null) {
        val s = _state.value
        val players = s.players
        if (players.isEmpty() || s.isGameOver) return

        val next = (1..players.size).map { (s.currentPlayerIndex + it) % players.size }
            .firstOrNull { !players[it].hasFinished } ?: return

        val nextPlayer = players[next]
        _state.update {
            it.copy(
                currentPlayerIndex = next,
                gamePhase = GamePhase.WAITING_FOR_ROLL,
                diceResult = null,
                validMoves = emptyList(),
                consecutiveSixes = 0,
                isDiceRollingForPlayer = null,
                hop = null,
                moveMessage = listOfNotNull(prefix, rollPrompt(nextPlayer)).joinToString(" ")
            )
        }
        continueTurn()
    }

    /** Kicks off the AI's roll when a bot is on turn; humans roll by tapping. */
    private fun continueTurn() {
        val s = _state.value
        val player = s.currentPlayer ?: return
        if (s.isGameOver || !player.isAI || s.gamePhase != GamePhase.WAITING_FOR_ROLL) return
        launch {
            delay(AI_ROLL_DELAY_MS)
            beginRoll()?.let { finishRoll(it) }
        }
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch(gameJob, block = block)
    }

    private fun rollPrompt(player: Player) = "${player.name}'s turn! Roll your dice 🎲"

    private fun PlayerColor.displayName() = name.lowercase().replaceFirstChar { it.uppercase() }

    // endregion

    companion object {
        private const val TAG = "GameEngine"
        private const val MAX_CONSECUTIVE_SIXES = 3

        const val ROLL_MS = 480L
        const val TURN_PAUSE_MS = 850L
        const val AUTO_MOVE_MS = 250L
        const val AI_THINK_MS = 450L
        const val AI_ROLL_DELAY_MS = 550L
        const val STEP_HOP_MS = 170
        const val LEAVE_BASE_HOP_MS = 300
        const val CAPTURE_HOP_MS = 450
    }
}

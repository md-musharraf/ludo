package com.example.ludo.engine

import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameEngine {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val moveValidator = MoveValidator()
    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var aiPlayer: AIPlayer? = null

    fun resetGame(playerCount: Int, isVsAI: Boolean, aiDifficulty: String = "Hard") {
        val selectedColors = when (playerCount) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW) // Opposite sides
            3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
            else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
        }

        val players = mutableListOf<Player>()
        for (i in selectedColors.indices) {
            val color = selectedColors[i]
            val isAI = isVsAI && i > 0
            val colorOrdinal = color.ordinal
            val tokens = List(4) { tokenId ->
                Token(
                    id = tokenId,
                    playerId = colorOrdinal,
                    boardPosition = BoardConfig.homePositions[colorOrdinal]?.get(tokenId)
                )
            }
            val defaultName = if (isAI) "Bot ${color.name.lowercase().replaceFirstChar { it.uppercase() }}"
            else "Player ${i + 1}"

            players.add(
                Player(
                    id = colorOrdinal,
                    color = color,
                    name = defaultName,
                    isAI = isAI,
                    tokens = tokens
                )
            )
        }

        _state.value = GameState(
            players = players,
            currentPlayerIndex = 0,
            gamePhase = GamePhase.WAITING_FOR_ROLL,
            moveMessage = "${players[0].name}'s turn! Roll your corner dice 🎲"
        )

        aiPlayer = if (isVsAI) AIPlayer(this, aiDifficulty) else null

        checkAI()
    }

    fun rollDice() {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.WAITING_FOR_ROLL || currentState.isGameOver) return

        val currentPlayer = currentState.players[currentState.currentPlayerIndex]

        // Start tumbling animation on this specific player's corner dice
        _state.update {
            it.copy(
                isDiceRollingForPlayer = currentPlayer.id,
                moveMessage = "${currentPlayer.name} is rolling..."
            )
        }

        SoundEffectManager.playDiceRoll()

        engineScope.launch {
            delay(500) // Realistic dice tumble duration

            val roll = (1..6).random()
            if (roll == 6) {
                SoundEffectManager.playSixRolled()
            }

            val stateWithRoll = _state.value.copy(
                diceResult = DiceResult(roll),
                isDiceRollingForPlayer = null
            )
            val validMoves = moveValidator.getValidMoves(stateWithRoll)

            if (validMoves.isEmpty()) {
                _state.update {
                    it.copy(
                        diceResult = DiceResult(roll),
                        isDiceRollingForPlayer = null,
                        consecutiveSixes = 0,
                        validMoves = emptyList(),
                        moveMessage = "Rolled a $roll. No valid moves!"
                    )
                }
                delay(1000)
                nextTurn()
            } else {
                val consecutive = if (roll == 6) currentState.consecutiveSixes + 1 else 0
                if (consecutive >= 3) {
                    _state.update {
                        it.copy(
                            diceResult = DiceResult(roll),
                            isDiceRollingForPlayer = null,
                            consecutiveSixes = 0,
                            validMoves = emptyList(),
                            moveMessage = "Three 6s! Turn forfeited ⚠️"
                        )
                    }
                    delay(1000)
                    nextTurn()
                } else {
                    val extraRollText = if (roll == 6) " (Bonus roll on 6!)" else ""
                    _state.update {
                        it.copy(
                            diceResult = DiceResult(roll),
                            isDiceRollingForPlayer = null,
                            validMoves = validMoves,
                            consecutiveSixes = consecutive,
                            gamePhase = GamePhase.WAITING_FOR_MOVE,
                            moveMessage = "${currentPlayer.name} rolled a $roll! Tap a glowing piece$extraRollText"
                        )
                    }

                    if (currentPlayer.isAI) {
                        aiPlayer?.executeMove(validMoves)
                    }
                }
            }
        }
    }

    fun selectToken(tokenId: Int) {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.WAITING_FOR_MOVE || currentState.isGameOver) return
        if (!currentState.validMoves.contains(tokenId)) return

        val playerIndex = currentState.currentPlayerIndex
        val player = currentState.players[playerIndex]
        val colorOrdinal = player.color.ordinal
        val tokenIndex = player.tokens.indexOfFirst { it.id == tokenId }
        if (tokenIndex == -1) return

        val token = player.tokens[tokenIndex]
        val diceRoll = currentState.diceResult?.value ?: return

        _state.update {
            it.copy(
                gamePhase = GamePhase.ANIMATING_MOVE,
                animatingTokenId = tokenId,
                animatingPlayerId = player.id,
                validMoves = emptyList(),
                moveMessage = "${player.name} is moving piece ${tokenId + 1}..."
            )
        }

        engineScope.launch {
            val path = PathMapper.getPlayerPath(colorOrdinal)

            if (token.state == TokenState.IN_HOME) {
                // Leaving home to start square
                SoundEffectManager.playLeaveBase()
                val targetBoardPos = path[0]
                
                // Parabolic step animation from home spot to start square
                val homeSpot = BoardConfig.homePositions[colorOrdinal]?.get(token.id) ?: Pair(0,0)
                animateHopFrames(player.id, tokenId, homeSpot, targetBoardPos)

                val updatedToken = token.copy(
                    state = TokenState.ON_BOARD,
                    positionIndex = 0,
                    boardPosition = targetBoardPos
                )
                updateToken(playerIndex, tokenIndex, updatedToken)
                delay(200)
                handlePostMove(playerIndex, updatedToken)
            } else {
                // Step-by-step parabolic hop
                var currentPos = token.positionIndex
                for (step in 1..diceRoll) {
                    val fromPos = path[currentPos]
                    currentPos++
                    val toPos = path[currentPos]

                    SoundEffectManager.playTokenStep()
                    animateHopFrames(player.id, tokenId, fromPos, toPos)

                    val state = if (currentPos >= 51) TokenState.IN_HOME_COLUMN else TokenState.ON_BOARD
                    val finalState = if (currentPos == 56) TokenState.FINISHED else state

                    val updatedToken = token.copy(
                        state = finalState,
                        positionIndex = currentPos,
                        boardPosition = toPos
                    )
                    updateToken(playerIndex, tokenIndex, updatedToken)
                }
                val finalToken = _state.value.players[playerIndex].tokens[tokenIndex]
                handlePostMove(playerIndex, finalToken)
            }
        }
    }

    private suspend fun animateHopFrames(
        playerId: Int,
        tokenId: Int,
        fromPos: Pair<Int, Int>,
        toPos: Pair<Int, Int>
    ) {
        val frames = 5
        for (f in 1..frames) {
            val progress = f.toFloat() / frames.toFloat()
            _state.update {
                it.copy(
                    animatingPlayerId = playerId,
                    animatingTokenId = tokenId,
                    animatingFromPos = fromPos,
                    animatingToPos = toPos,
                    animatingHopProgress = progress
                )
            }
            delay(32) // ~30fps smooth hop interpolation
        }
        _state.update {
            it.copy(
                animatingFromPos = null,
                animatingToPos = null,
                animatingHopProgress = 0f
            )
        }
    }

    private fun updateToken(playerIndex: Int, tokenIndex: Int, token: Token) {
        val players = _state.value.players.toMutableList()
        val player = players[playerIndex]
        val tokens = player.tokens.toMutableList()
        tokens[tokenIndex] = token

        val hasFinished = tokens.all { it.state == TokenState.FINISHED }
        players[playerIndex] = player.copy(tokens = tokens, hasFinished = hasFinished)
        _state.update { it.copy(players = players) }
    }

    private suspend fun handlePostMove(playerIndex: Int, token: Token) {
        val currentPlayer = _state.value.players[playerIndex]
        var extraTurn = false
        var captureMessage = ""

        if (_state.value.diceResult?.value == 6) {
            extraTurn = true
        }

        if (token.state == TokenState.FINISHED) {
            extraTurn = true
            captureMessage = " 🎉 ${currentPlayer.name} reached HOME!"
            SoundEffectManager.playSixRolled()
        }

        // Capture check
        if (token.state == TokenState.ON_BOARD && !BoardConfig.safePositions.contains(token.boardPosition)) {
            val players = _state.value.players.toMutableList()
            var captured = false

            for (i in players.indices) {
                if (i == playerIndex) continue
                val opp = players[i]
                val oppColorOrdinal = opp.color.ordinal
                val oppTokens = opp.tokens.toMutableList()
                var oppUpdated = false

                for (j in oppTokens.indices) {
                    val oppToken = oppTokens[j]
                    if (oppToken.state == TokenState.ON_BOARD && oppToken.boardPosition == token.boardPosition) {
                        SoundEffectManager.playCapture()
                        val homeDest = BoardConfig.homePositions[oppColorOrdinal]?.get(oppToken.id) ?: Pair(0,0)
                        
                        // Set capture event for flyback animation
                        val captureEvent = CapturedTokenEvent(
                            playerId = opp.id,
                            tokenId = oppToken.id,
                            fromPosition = oppToken.boardPosition ?: Pair(0,0),
                            toHomePosition = homeDest
                        )
                        _state.update { it.copy(lastCapturedEvent = captureEvent) }

                        oppTokens[j] = oppToken.copy(
                            state = TokenState.IN_HOME,
                            positionIndex = -1,
                            boardPosition = homeDest
                        )
                        captured = true
                        oppUpdated = true
                        extraTurn = true
                    }
                }
                if (oppUpdated) {
                    players[i] = opp.copy(tokens = oppTokens)
                }
            }

            if (captured) {
                captureMessage = " 💥 CAPTURED an opponent! Bonus turn!"
                _state.update { it.copy(players = players) }
                delay(600) // Dramatic pause for flyback
            }
        }

        _state.update {
            it.copy(
                animatingTokenId = null,
                animatingPlayerId = null,
                lastCapturedEvent = null
            )
        }

        checkWin()

        if (_state.value.isGameOver) return

        if (extraTurn) {
            _state.update {
                it.copy(
                    gamePhase = GamePhase.WAITING_FOR_ROLL,
                    validMoves = emptyList(),
                    diceResult = null,
                    moveMessage = "${currentPlayer.name} gets a bonus turn!$captureMessage Roll corner dice 🎲"
                )
            }
            checkAI()
        } else {
            nextTurn()
        }
    }

    private fun nextTurn() {
        val players = _state.value.players
        if (players.isEmpty()) return

        var next = (_state.value.currentPlayerIndex + 1) % players.size
        var loopCount = 0
        while (players[next].hasFinished && loopCount < players.size) {
            next = (next + 1) % players.size
            loopCount++
        }

        val nextPlayer = players[next]
        _state.update {
            it.copy(
                currentPlayerIndex = next,
                gamePhase = GamePhase.WAITING_FOR_ROLL,
                diceResult = null,
                validMoves = emptyList(),
                consecutiveSixes = 0,
                isDiceRollingForPlayer = null,
                animatingTokenId = null,
                animatingPlayerId = null,
                moveMessage = "${nextPlayer.name}'s turn! Roll your corner dice 🎲"
            )
        }
        checkAI()
    }

    private fun checkWin() {
        val players = _state.value.players
        val winner = players.firstOrNull { it.hasFinished }
        if (winner != null) {
            SoundEffectManager.playWinFanfare()
            _state.update {
                it.copy(
                    gamePhase = GamePhase.GAME_OVER,
                    isGameOver = true,
                    winnerId = winner.id,
                    moveMessage = "🏆 ${winner.name} WINS THE MATCH!"
                )
            }
        }
    }

    private fun checkAI() {
        val state = _state.value
        if (state.isGameOver || state.players.isEmpty()) return
        val player = state.players[state.currentPlayerIndex]
        if (player.isAI && state.gamePhase == GamePhase.WAITING_FOR_ROLL) {
            engineScope.launch {
                aiPlayer?.executeRoll()
            }
        }
    }

    fun getValidMoves(): List<Int> = _state.value.validMoves
}

package com.example.ludo.engine

import com.example.ludo.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/** Deterministic dice: returns the queued rolls in order. */
private class FixedRandom(vararg rolls: Int) : Random() {
    private val queue = ArrayDeque(rolls.toList())
    var rollsUsed = 0
        private set

    override fun nextBits(bitCount: Int): Int = 0
    override fun nextInt(from: Int, until: Int): Int {
        rollsUsed++
        return queue.removeFirst()
    }
}

private fun player(color: PlayerColor, tokens: List<Token>, isAI: Boolean = false) =
    Player(id = color.ordinal, color = color, name = color.name, isAI = isAI, tokens = tokens)

private fun homeTokens(color: PlayerColor, count: Int = 4, startId: Int = 0) =
    List(count) { Token(id = startId + it, playerId = color.ordinal, boardPosition = BoardConfig.homeSpot(color.ordinal, startId + it)) }

private fun tokenAt(color: PlayerColor, id: Int, pathIndex: Int): Token {
    val state = when {
        pathIndex >= BoardConfig.GOAL_INDEX -> TokenState.FINISHED
        pathIndex >= BoardConfig.HOME_COLUMN_START -> TokenState.IN_HOME_COLUMN
        else -> TokenState.ON_BOARD
    }
    return Token(id, color.ordinal, state, pathIndex, PathMapper.getPlayerPath(color.ordinal)[pathIndex])
}

/** Path index on [color]'s route of the given shared-track cell. */
private fun pathIndexOf(color: PlayerColor, cell: Pair<Int, Int>) =
    PathMapper.getPlayerPath(color.ordinal).indexOf(cell)

@OptIn(ExperimentalCoroutinesApi::class)
class LudoEngineTest {

    /**
     * Engine on the test scheduler. Not [TestScope.backgroundScope]: advanceUntilIdle() ignores
     * background work. The scope is cancelled when the test ends so AI turns can't outlive it.
     */
    private fun TestScope.engine(random: Random = Random(42)): GameEngine {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        backgroundScope.launch {
            try {
                awaitCancellation()
            } finally {
                scope.cancel()
            }
        }
        return GameEngine(scope, random)
    }

    // region Board & paths

    @Test
    fun testBoardConfigTrackSize() {
        assertEquals("Main track must have exactly 52 coordinates", 52, BoardConfig.mainTrack.size)
        assertEquals("Track cells must be unique", 52, BoardConfig.mainTrack.toSet().size)
        assertEquals("There must be 8 safe spot indices", 8, BoardConfig.safeSpotsIndices.size)
        assertEquals("There must be 4 home base positions", 4, BoardConfig.homePositions.size)
    }

    @Test
    fun testPathMapperPrecomputedZeroAllocationIdentity() {
        assertSame(PathMapper.getPlayerPath(0), PathMapper.getPlayerPath(0))
    }

    @Test
    fun testPathsStartAndFinishForEveryColour() {
        val expected = mapOf(
            PlayerColor.RED to (Pair(6, 1) to Pair(7, 6)),
            PlayerColor.GREEN to (Pair(1, 8) to Pair(6, 7)),
            PlayerColor.YELLOW to (Pair(8, 13) to Pair(7, 8)),
            PlayerColor.BLUE to (Pair(13, 6) to Pair(8, 7))
        )
        for ((color, ends) in expected) {
            val path = PathMapper.getPlayerPath(color.ordinal)
            assertEquals("$color path must have 57 positions", 57, path.size)
            assertEquals("$color start", ends.first, path[0])
            assertEquals("$color finish", ends.second, path[56])
            // Consecutive path cells must be orthogonally adjacent (no teleporting hops).
            for (i in 1 until BoardConfig.HOME_COLUMN_START) {
                val (r1, c1) = path[i - 1]
                val (r2, c2) = path[i]
                assertTrue("$color step $i is not a single-cell move", kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2) <= 2)
            }
        }
    }

    // endregion

    // region Move validation

    @Test
    fun testMoveValidatorInHome() {
        val validator = MoveValidator()
        val tokenInHome = Token(id = 0, playerId = 0, state = TokenState.IN_HOME)
        val state = GameState(players = listOf(player(PlayerColor.RED, listOf(tokenInHome))))

        assertFalse(validator.isValidMove(tokenInHome, 1, state))
        assertFalse(validator.isValidMove(tokenInHome, 5, state))
        assertTrue(validator.isValidMove(tokenInHome, 6, state))
    }

    @Test
    fun testMoveValidatorExactFinish() {
        val validator = MoveValidator()
        val token = tokenAt(PlayerColor.RED, 0, 54)
        val state = GameState(players = listOf(player(PlayerColor.RED, listOf(token))))

        assertTrue("Roll 2 from 54 reaches the goal", validator.isValidMove(token, 2, state))
        assertTrue(validator.isValidMove(token, 1, state))
        assertFalse("Roll 3 from 54 overshoots", validator.isValidMove(token, 3, state))
    }

    @Test
    fun testCannotLandOnBlockadeButCanLandOnSingleOpponent() {
        val validator = MoveValidator()
        val redPath = PathMapper.getPlayerPath(0)
        val target = redPath[5] // Not a safe spot
        val green = PlayerColor.GREEN
        val greenIdx = pathIndexOf(green, target)
        val red = tokenAt(PlayerColor.RED, 0, 2)

        val blockade = GameState(
            players = listOf(
                player(PlayerColor.RED, listOf(red)),
                player(green, listOf(tokenAt(green, 0, greenIdx), tokenAt(green, 1, greenIdx)))
            )
        )
        assertFalse("Cannot land on a blockade", validator.isValidMove(red, 3, blockade))

        val single = blockade.copy(players = listOf(blockade.players[0], player(green, listOf(tokenAt(green, 0, greenIdx)))))
        assertTrue("A lone opponent can be landed on (captured)", validator.isValidMove(red, 3, single))
    }

    @Test
    fun testValidMovesForSixInHome() {
        val state = GameState(
            players = listOf(player(PlayerColor.RED, homeTokens(PlayerColor.RED))),
            diceResult = DiceResult(6),
            gamePhase = GamePhase.WAITING_FOR_MOVE
        )
        assertEquals(listOf(0, 1, 2, 3), MoveValidator().getValidMoves(state))
    }

    // endregion

    // region Game setup

    @Test
    fun testGameEngineReset() = runTest {
        val engine = engine()
        engine.resetGame(playerCount = 4, isVsAI = true, aiDifficulty = AiDifficulty.HARD)

        val state = engine.state.value
        assertEquals(4, state.players.size)
        assertFalse("First player must be human in AI mode", state.players[0].isAI)
        assertTrue("Other players must be AI in AI mode", state.players.drop(1).all { it.isAI })
        assertEquals(GamePhase.WAITING_FOR_ROLL, state.gamePhase)
        assertFalse(state.isGameOver)
    }

    @Test
    fun testPlayerCountColours() = runTest {
        val engine = engine()
        engine.resetGame(playerCount = 2, isVsAI = false)
        assertEquals(listOf(PlayerColor.RED, PlayerColor.YELLOW), engine.state.value.players.map { it.color })

        engine.resetGame(playerCount = 3, isVsAI = false)
        assertEquals(listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW), engine.state.value.players.map { it.color })
    }

    @Test
    fun testDifficultyParsingIsLenient() {
        assertEquals(AiDifficulty.EASY, AiDifficulty.fromName("easy"))
        assertEquals(AiDifficulty.HARD, AiDifficulty.fromName("Hard"))
        assertEquals("Unknown input falls back to HARD", AiDifficulty.HARD, AiDifficulty.fromName("<script>"))
        assertEquals(AiDifficulty.HARD, AiDifficulty.fromName(null))
    }

    // endregion

    // region Turn flow

    @Test
    fun testDoubleTapRollsOnlyOnce() = runTest {
        val dice = FixedRandom(3, 4)
        val engine = engine(dice)
        engine.resetGame(playerCount = 2, isVsAI = false)

        engine.rollDice()
        engine.rollDice()
        advanceTimeBy(GameEngine.ROLL_MS + 1)
        assertEquals("Second tap during a roll must be ignored", 1, dice.rollsUsed)
    }

    @Test
    fun testNoValidMovePassesTurn() = runTest {
        val engine = engine(FixedRandom(3))
        engine.resetGame(playerCount = 2, isVsAI = false)

        engine.rollDice()
        advanceUntilIdle()
        assertEquals("Turn passes to Yellow", 1, engine.state.value.currentPlayerIndex)
        assertEquals(GamePhase.WAITING_FOR_ROLL, engine.state.value.gamePhase)
    }

    @Test
    fun testHumanCannotActDuringAiTurn() = runTest {
        val engine = engine(FixedRandom(3, 3))
        engine.resetGame(playerCount = 2, isVsAI = true)

        engine.rollDice()
        advanceTimeBy(GameEngine.ROLL_MS + GameEngine.TURN_PAUSE_MS + 1)
        val aiTurn = engine.state.value
        assertTrue("Bot is now on turn", aiTurn.currentPlayer!!.isAI)

        engine.rollDice()
        assertNull("Human tap must not roll for the bot", engine.state.value.isDiceRollingForPlayer)
    }

    @Test
    fun testHumanCannotMoveAiToken() = runTest {
        val engine = engine(FixedRandom())
        val bot = player(PlayerColor.YELLOW, listOf(tokenAt(PlayerColor.YELLOW, 0, 5)) + homeTokens(PlayerColor.YELLOW, 3, 1), isAI = true)
        engine.loadState(
            GameState(
                players = listOf(player(PlayerColor.RED, homeTokens(PlayerColor.RED)), bot),
                currentPlayerIndex = 1,
                gamePhase = GamePhase.WAITING_FOR_MOVE,
                diceResult = DiceResult(2),
                validMoves = listOf(0)
            )
        )
        engine.selectToken(0)
        assertEquals("Engine must ignore a human selecting a bot token", GamePhase.WAITING_FOR_MOVE, engine.state.value.gamePhase)
    }

    @Test
    fun testIdenticalChoicesAutoMove() = runTest {
        val engine = engine(FixedRandom(6))
        engine.resetGame(playerCount = 2, isVsAI = false)

        engine.rollDice()
        advanceTimeBy(GameEngine.ROLL_MS + GameEngine.AUTO_MOVE_MS + 1)
        assertEquals("Four identical base tokens: no tap needed", GamePhase.ANIMATING_MOVE, engine.state.value.gamePhase)
        advanceUntilIdle()

        val red = engine.state.value.players[0]
        assertEquals(TokenState.ON_BOARD, red.tokens[0].state)
        assertEquals("Rolling a 6 keeps the turn", 0, engine.state.value.currentPlayerIndex)
    }

    @Test
    fun testMoveEmitsHopsThenCommits() = runTest {
        val engine = engine(FixedRandom(3))
        engine.loadState(
            GameState(players = listOf(
                player(PlayerColor.RED, listOf(tokenAt(PlayerColor.RED, 0, 10)) + homeTokens(PlayerColor.RED, 3, 1)),
                player(PlayerColor.YELLOW, homeTokens(PlayerColor.YELLOW))
            ))
        )
        engine.rollDice()
        advanceTimeBy(GameEngine.ROLL_MS + GameEngine.AUTO_MOVE_MS + 1)
        runCurrent()
        assertNotNull("A hop is in flight during the move", engine.state.value.hop)

        advanceUntilIdle()
        val token = engine.state.value.players[0].tokens[0]
        assertEquals(13, token.positionIndex)
        assertEquals(PathMapper.getPlayerPath(0)[13], token.boardPosition)
        assertNull("Hop cleared after the move", engine.state.value.hop)
    }

    @Test
    fun testThreeSixesForfeitsTurn() = runTest {
        val engine = engine(FixedRandom(6))
        engine.loadState(
            GameState(
                players = listOf(
                    player(PlayerColor.RED, listOf(tokenAt(PlayerColor.RED, 0, 10)) + homeTokens(PlayerColor.RED, 3, 1)),
                    player(PlayerColor.YELLOW, homeTokens(PlayerColor.YELLOW))
                ),
                consecutiveSixes = 2
            )
        )
        engine.rollDice()
        advanceUntilIdle()

        val state = engine.state.value
        assertEquals("Turn passes after the third six", 1, state.currentPlayerIndex)
        assertEquals(0, state.consecutiveSixes)
        assertEquals("No piece moved", 10, state.players[0].tokens[0].positionIndex)
    }

    @Test
    fun testCaptureSendsOpponentHomeAndGrantsBonus() = runTest {
        val engine = engine(FixedRandom(2))
        val target = PathMapper.getPlayerPath(0)[12]
        assertFalse("Test needs a non-safe target", BoardConfig.isSafe(target))
        val yellow = PlayerColor.YELLOW

        engine.loadState(
            GameState(players = listOf(
                player(PlayerColor.RED, listOf(tokenAt(PlayerColor.RED, 0, 10)) + homeTokens(PlayerColor.RED, 3, 1)),
                player(yellow, listOf(tokenAt(yellow, 0, pathIndexOf(yellow, target))) + homeTokens(yellow, 3, 1))
            ))
        )
        engine.rollDice()
        advanceUntilIdle()

        val state = engine.state.value
        val victim = state.players[1].tokens[0]
        assertEquals(TokenState.IN_HOME, victim.state)
        assertEquals(-1, victim.positionIndex)
        assertEquals(BoardConfig.homeSpot(yellow.ordinal, 0), victim.boardPosition)
        assertEquals("Capture grants a bonus turn", 0, state.currentPlayerIndex)
        assertEquals(GamePhase.WAITING_FOR_ROLL, state.gamePhase)
    }

    @Test
    fun testNoCaptureOnSafeSquare() = runTest {
        val engine = engine(FixedRandom(3))
        val safe = PathMapper.getPlayerPath(0)[8] // Star square
        assertTrue(BoardConfig.isSafe(safe))
        val yellow = PlayerColor.YELLOW

        engine.loadState(
            GameState(players = listOf(
                player(PlayerColor.RED, listOf(tokenAt(PlayerColor.RED, 0, 5)) + homeTokens(PlayerColor.RED, 3, 1)),
                player(yellow, listOf(tokenAt(yellow, 0, pathIndexOf(yellow, safe))) + homeTokens(yellow, 3, 1))
            ))
        )
        engine.rollDice()
        advanceUntilIdle()

        val state = engine.state.value
        assertEquals("Opponent stays on the safe square", TokenState.ON_BOARD, state.players[1].tokens[0].state)
        assertEquals("No bonus: turn passes", 1, state.currentPlayerIndex)
    }

    // endregion

    // region Finishing & ranking

    private fun nearlyDone(color: PlayerColor, isAI: Boolean = false) = player(
        color,
        List(3) { tokenAt(color, it, BoardConfig.GOAL_INDEX) } + tokenAt(color, 3, 55),
        isAI
    )

    @Test
    fun testTwoPlayerGameEndsWhenOneFinishes() = runTest {
        val engine = engine(FixedRandom(1))
        engine.loadState(GameState(players = listOf(nearlyDone(PlayerColor.RED), player(PlayerColor.YELLOW, homeTokens(PlayerColor.YELLOW)))))

        engine.rollDice()
        advanceUntilIdle()

        val state = engine.state.value
        assertTrue(state.isGameOver)
        assertEquals(PlayerColor.RED.ordinal, state.winnerId)
        assertEquals(listOf(1, 2), state.standings.map { it.rank })
    }

    @Test
    fun testThreePlayerGameContinuesForRunnerUp() = runTest {
        val engine = engine(FixedRandom(1))
        engine.loadState(
            GameState(players = listOf(
                nearlyDone(PlayerColor.RED),
                player(PlayerColor.GREEN, homeTokens(PlayerColor.GREEN)),
                player(PlayerColor.YELLOW, homeTokens(PlayerColor.YELLOW))
            ))
        )
        engine.rollDice()
        advanceUntilIdle()

        val state = engine.state.value
        assertFalse("Two players still racing for 2nd", state.isGameOver)
        assertEquals(1, state.players[0].rank)
        assertEquals("Finished player is skipped", 1, state.currentPlayerIndex)
    }

    @Test
    fun testVsAiEndsWhenHumanFinishes() = runTest {
        val engine = engine(FixedRandom(1))
        engine.loadState(
            GameState(players = listOf(
                nearlyDone(PlayerColor.RED),
                player(PlayerColor.GREEN, homeTokens(PlayerColor.GREEN), isAI = true),
                player(PlayerColor.YELLOW, homeTokens(PlayerColor.YELLOW), isAI = true)
            ))
        )
        engine.rollDice()
        advanceUntilIdle()

        val state = engine.state.value
        assertTrue("Bots racing each other is pointless once the human is done", state.isGameOver)
        assertEquals(PlayerColor.RED.ordinal, state.winnerId)
        assertEquals(3, state.standings.size)
    }

    // endregion
}

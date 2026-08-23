package com.example.ludo.engine

import com.example.ludo.model.*
import org.junit.Assert.*
import org.junit.Test

class LudoEngineTest {

    @Test
    fun testBoardConfigTrackSize() {
        assertEquals("Main track must have exactly 52 coordinates", 52, BoardConfig.mainTrack.size)
        assertEquals("There must be 8 safe spot indices", 8, BoardConfig.safeSpotsIndices.size)
        assertEquals("There must be 4 home base positions", 4, BoardConfig.homePositions.size)
    }

    @Test
    fun testPathMapperForRed() {
        val redPath = PathMapper.getPlayerPath(0) // Red
        assertEquals("Player path must have 57 positions (51 track + 6 home)", 57, redPath.size)
        assertEquals("Red starting square must be (6, 1)", Pair(6, 1), redPath[0])
        assertEquals("Red finish square must be (7, 6)", Pair(7, 6), redPath[56])
    }

    @Test
    fun testPathMapperForGreen() {
        val greenPath = PathMapper.getPlayerPath(1) // Green
        assertEquals("Player path must have 57 positions", 57, greenPath.size)
        assertEquals("Green starting square must be (1, 8)", Pair(1, 8), greenPath[0])
        assertEquals("Green finish square must be (6, 7)", Pair(6, 7), greenPath[56])
    }

    @Test
    fun testPathMapperForYellow() {
        val yellowPath = PathMapper.getPlayerPath(2) // Yellow
        assertEquals("Player path must have 57 positions", 57, yellowPath.size)
        assertEquals("Yellow starting square must be (8, 13)", Pair(8, 13), yellowPath[0])
        assertEquals("Yellow finish square must be (7, 8)", Pair(7, 8), yellowPath[56])
    }

    @Test
    fun testPathMapperForBlue() {
        val bluePath = PathMapper.getPlayerPath(3) // Blue
        assertEquals("Player path must have 57 positions", 57, bluePath.size)
        assertEquals("Blue starting square must be (13, 6)", Pair(13, 6), bluePath[0])
        assertEquals("Blue finish square must be (8, 7)", Pair(8, 7), bluePath[56])
    }

    @Test
    fun testMoveValidatorInHome() {
        val validator = MoveValidator()
        val tokenInHome = Token(id = 0, playerId = 0, state = TokenState.IN_HOME)
        val dummyState = GameState()

        assertFalse("Cannot move token out of home with a 1", validator.isValidMove(tokenInHome, 1, dummyState))
        assertFalse("Cannot move token out of home with a 5", validator.isValidMove(tokenInHome, 5, dummyState))
        assertTrue("Must be able to move token out of home with a 6", validator.isValidMove(tokenInHome, 6, dummyState))
    }

    @Test
    fun testMoveValidatorExactFinish() {
        val validator = MoveValidator()
        val tokenNearFinish = Token(id = 0, playerId = 0, state = TokenState.IN_HOME_COLUMN, positionIndex = 54)
        val dummyState = GameState()

        assertTrue("Roll 2 from index 54 reaches index 56 (finish)", validator.isValidMove(tokenNearFinish, 2, dummyState))
        assertTrue("Roll 1 from index 54 reaches index 55", validator.isValidMove(tokenNearFinish, 1, dummyState))
        assertFalse("Roll 3 from index 54 overshoots index 56 (>56)", validator.isValidMove(tokenNearFinish, 3, dummyState))
    }

    @Test
    fun testGameEngineReset() {
        val engine = GameEngine()
        engine.resetGame(playerCount = 4, isVsAI = true, aiDifficulty = "Hard")

        val state = engine.state.value
        assertEquals("Must initialize with 4 players", 4, state.players.size)
        assertEquals("First player must be human in AI mode", false, state.players[0].isAI)
        assertEquals("Other players must be AI in AI mode", true, state.players[1].isAI)
        assertEquals("Game phase must be WAITING_FOR_ROLL", GamePhase.WAITING_FOR_ROLL, state.gamePhase)
        assertFalse("Game must not be over initially", state.isGameOver)
    }

    @Test
    fun testTwoPlayerModeOppositeColors() {
        val engine = GameEngine()
        engine.resetGame(playerCount = 2, isVsAI = false)

        val state = engine.state.value
        assertEquals("Must have 2 players", 2, state.players.size)
        assertEquals("Player 1 should be RED", PlayerColor.RED, state.players[0].color)
        assertEquals("Player 2 should be YELLOW (opposite)", PlayerColor.YELLOW, state.players[1].color)
    }

    @Test
    fun testCapturedTokenEventStructure() {
        val event = CapturedTokenEvent(
            playerId = 1,
            tokenId = 2,
            fromPosition = Pair(6, 1),
            toHomePosition = Pair(2, 11)
        )
        assertEquals(1, event.playerId)
        assertEquals(2, event.tokenId)
        assertEquals(Pair(6, 1), event.fromPosition)
        assertEquals(Pair(2, 11), event.toHomePosition)
    }
}

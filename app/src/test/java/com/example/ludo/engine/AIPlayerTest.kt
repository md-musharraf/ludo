package com.example.ludo.engine

import com.example.ludo.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AIPlayerTest {

    private val ai = AIPlayer(AiDifficulty.HARD)

    private fun token(color: PlayerColor, id: Int, pathIndex: Int) = Token(
        id = id,
        playerId = color.ordinal,
        state = if (pathIndex >= BoardConfig.HOME_COLUMN_START) TokenState.IN_HOME_COLUMN else TokenState.ON_BOARD,
        positionIndex = pathIndex,
        boardPosition = PathMapper.getPlayerPath(color.ordinal)[pathIndex]
    )

    private fun player(color: PlayerColor, vararg tokens: Token, isAI: Boolean = false) =
        Player(id = color.ordinal, color = color, name = color.name, isAI = isAI, tokens = tokens.toList())

    private fun state(dice: Int, vararg players: Player) =
        GameState(players = players.toList(), diceResult = DiceResult(dice), gamePhase = GamePhase.WAITING_FOR_MOVE)

    private fun yellowIndexOf(cell: Pair<Int, Int>) = PathMapper.getPlayerPath(PlayerColor.YELLOW.ordinal).indexOf(cell)

    @Test
    fun noMovesReturnsNull() {
        assertNull(ai.chooseMove(state(3, player(PlayerColor.RED, isAI = true)), emptyList()))
    }

    @Test
    fun prefersFinishingOverAdvancing() {
        val bot = player(PlayerColor.RED, token(PlayerColor.RED, 0, 20), token(PlayerColor.RED, 1, 53), isAI = true)
        assertEquals(1, ai.chooseMove(state(3, bot), listOf(0, 1)))
    }

    @Test
    fun prefersCaptureOverPlainMove() {
        val redPath = PathMapper.getPlayerPath(PlayerColor.RED.ordinal)
        val victimCell = redPath[24] // 20 + 4, not a safe square
        val bot = player(PlayerColor.RED, token(PlayerColor.RED, 0, 20), token(PlayerColor.RED, 1, 30), isAI = true)
        val opponent = player(PlayerColor.YELLOW, token(PlayerColor.YELLOW, 0, yellowIndexOf(victimCell)))
        assertEquals(0, ai.chooseMove(state(4, bot, opponent), listOf(0, 1)))
    }

    @Test
    fun avoidsLandingRightInFrontOfAnOpponent() {
        val redPath = PathMapper.getPlayerPath(PlayerColor.RED.ordinal)
        // Token 0 would land one square ahead of a Yellow piece; token 1 lands out of reach.
        val bot = player(PlayerColor.RED, token(PlayerColor.RED, 0, 22), token(PlayerColor.RED, 1, 36), isAI = true)
        val hunter = player(PlayerColor.YELLOW, token(PlayerColor.YELLOW, 0, yellowIndexOf(redPath[23])))
        assertEquals(1, ai.chooseMove(state(2, bot, hunter), listOf(0, 1)))
    }

    @Test
    fun threatCountIgnoresSafeSquaresAndOwnPieces() {
        val redPath = PathMapper.getPlayerPath(PlayerColor.RED.ordinal)
        val red = player(PlayerColor.RED, token(PlayerColor.RED, 0, 3))
        val yellow = player(PlayerColor.YELLOW, token(PlayerColor.YELLOW, 0, yellowIndexOf(redPath[5])))
        val players = listOf(red, yellow)

        assertEquals("Safe start square is never threatened", 0, AIPlayer.threatsAt(redPath[0], red, players))
        assertEquals("Yellow two squares behind threatens", 1, AIPlayer.threatsAt(redPath[7], red, players))
        assertEquals("Squares behind Yellow are not threatened by it", 0, AIPlayer.threatsAt(redPath[4], red, players))
    }
}

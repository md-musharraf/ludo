package com.example.ludo.engine

import com.example.ludo.model.GameState
import com.example.ludo.model.Player
import com.example.ludo.model.Token
import com.example.ludo.model.TokenState

/**
 * Validates Ludo token movements:
 * - Rolling a 6 is required to enter the track from home base.
 * - A move must land exactly on or before the goal (index 56); no overshoot.
 * - An opponent blockade (2+ tokens of one colour on a non-safe cell) cannot be landed on.
 */
class MoveValidator {
    fun getValidMoves(gameState: GameState): List<Int> {
        val currentPlayer = gameState.currentPlayer ?: return emptyList()
        val diceValue = gameState.diceResult?.value ?: return emptyList()
        return currentPlayer.tokens.filter { isValidMove(it, diceValue, gameState) }.map { it.id }
    }

    fun isValidMove(token: Token, diceValue: Int, gameState: GameState): Boolean {
        if (diceValue !in 1..6) return false
        return when (token.state) {
            TokenState.FINISHED -> false
            TokenState.IN_HOME -> diceValue == 6
            TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                val targetPos = token.positionIndex + diceValue
                if (targetPos > BoardConfig.GOAL_INDEX) return false
                if (targetPos >= BoardConfig.HOME_COLUMN_START) return true // Private lane, nobody can block it

                val mover = gameState.currentPlayer ?: return false
                val targetCell = PathMapper.getPlayerPath(mover.color.ordinal)[targetPos]
                !isBlockade(targetCell, mover, gameState.players)
            }
        }
    }

    companion object {
        /** True when a single opponent colour has 2+ tokens on a non-safe [cell]. */
        fun isBlockade(cell: Pair<Int, Int>, mover: Player, players: List<Player>): Boolean {
            if (BoardConfig.isSafe(cell)) return false
            return players.any { other ->
                other.id != mover.id && other.tokens.count { it.isOnMainTrack && it.boardPosition == cell } >= 2
            }
        }
    }
}

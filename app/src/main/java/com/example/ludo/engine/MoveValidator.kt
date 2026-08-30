package com.example.ludo.engine

import com.example.ludo.model.GameState
import com.example.ludo.model.Token
import com.example.ludo.model.TokenState

/**
 * Validates Ludo token movements according to official rules:
 * - Rolling a 6 allows entering the track from home base.
 * - Move distance along precomputed path must not exceed the goal (position 56).
 * - Opponent blockades (2+ enemy tokens on a non-safe cell) prevent landing.
 */
class MoveValidator {
    fun getValidMoves(gameState: GameState): List<Int> {
        val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return emptyList()
        val diceResult = gameState.diceResult?.value ?: return emptyList()
        if (diceResult !in 1..6) return emptyList()

        val validTokenIds = ArrayList<Int>(4)
        for (token in currentPlayer.tokens) {
            if (isValidMove(token, diceResult, gameState)) {
                validTokenIds.add(token.id)
            }
        }
        return validTokenIds
    }

    fun isValidMove(token: Token, diceValue: Int, gameState: GameState): Boolean {
        if (token.state == TokenState.FINISHED) return false
        if (diceValue !in 1..6) return false

        if (token.state == TokenState.IN_HOME) {
            return diceValue == 6
        }

        val currentPos = token.positionIndex
        val targetPos = currentPos + diceValue

        // Cannot overshoot center goal (56 is final index)
        if (targetPos > 56) return false

        val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return false

        // Check for opponent blockade at target position (only applies on main track outside safe zones)
        if (targetPos < 51) {
            val colorOrdinal = currentPlayer.color.ordinal
            val path = PathMapper.getPlayerPath(colorOrdinal)
            if (targetPos in path.indices) {
                val targetBoardPos = path[targetPos]

                // Safe spots cannot be blockaded
                if (!BoardConfig.safePositions.contains(targetBoardPos)) {
                    for (otherPlayer in gameState.players) {
                        if (otherPlayer.id == currentPlayer.id) continue
                        var oppCountAtPos = 0
                        for (oppToken in otherPlayer.tokens) {
                            if ((oppToken.state == TokenState.ON_BOARD || oppToken.state == TokenState.IN_HOME_COLUMN) &&
                                oppToken.boardPosition == targetBoardPos
                            ) {
                                oppCountAtPos++
                            }
                        }
                        if (oppCountAtPos >= 2) return false // Blockade!
                    }
                }
            }
        }

        return true
    }
}


package com.example.ludo.engine

import com.example.ludo.model.GameState
import com.example.ludo.model.Token
import com.example.ludo.model.TokenState

class MoveValidator {
    fun getValidMoves(gameState: GameState): List<Int> {
        val currentPlayer = gameState.players[gameState.currentPlayerIndex]
        val diceResult = gameState.diceResult?.value ?: return emptyList()
        val validTokenIds = mutableListOf<Int>()

        for (token in currentPlayer.tokens) {
            if (isValidMove(token, diceResult, gameState)) {
                validTokenIds.add(token.id)
            }
        }

        return validTokenIds
    }

    fun isValidMove(token: Token, diceValue: Int, gameState: GameState): Boolean {
        if (token.state == TokenState.FINISHED) return false

        if (token.state == TokenState.IN_HOME) {
            return diceValue == 6
        }

        val currentPos = token.positionIndex
        val targetPos = currentPos + diceValue

        if (targetPos > 56) return false

        // Check for blockade at target position (only on main track, not home column)
        if (targetPos < 51) {
            val colorOrdinal = gameState.players[gameState.currentPlayerIndex].color.ordinal
            val path = PathMapper.getPlayerPath(colorOrdinal)
            if (targetPos < path.size) {
                val targetBoardPos = path[targetPos]
                // Check if there's an opponent blockade at target
                for (otherPlayer in gameState.players) {
                    if (otherPlayer.id == gameState.players[gameState.currentPlayerIndex].id) continue
                    val oppTokensAtPos = otherPlayer.tokens.count { t ->
                        (t.state == TokenState.ON_BOARD || t.state == TokenState.IN_HOME_COLUMN) &&
                            t.boardPosition == targetBoardPos
                    }
                    if (oppTokensAtPos >= 2) return false // Blockade!
                }
            }
        }

        return true
    }
}

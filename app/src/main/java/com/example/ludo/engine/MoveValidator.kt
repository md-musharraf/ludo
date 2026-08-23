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
        
        if (targetPos > 56) return false // Needs exact roll to finish
        
        // Blockade check can be added here
        return true
    }
}

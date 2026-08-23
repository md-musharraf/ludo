package com.example.ludo.engine

import com.example.ludo.model.GamePhase
import com.example.ludo.model.Player
import com.example.ludo.model.Token
import com.example.ludo.model.TokenState
import kotlinx.coroutines.delay

class AIPlayer(private val engine: GameEngine, private val difficulty: String) {

    suspend fun executeRoll() {
        delay(600)
        engine.rollDice()
    }

    suspend fun executeMove(validMoves: List<Int>) {
        if (validMoves.isEmpty()) return
        delay(700)
        val chosenMove = chooseBestMove(validMoves)
        engine.selectToken(chosenMove)
    }

    private fun chooseBestMove(validMoves: List<Int>): Int {
        if (validMoves.size == 1) return validMoves.first()
        if (difficulty == "Easy") {
            return validMoves.random()
        }

        // Smart / Hard AI strategy
        val state = engine.state.value
        val currentPlayer = state.players[state.currentPlayerIndex]
        val diceVal = state.diceResult?.value ?: 1

        val path = PathMapper.getPlayerPath(currentPlayer.id)

        // Evaluate each possible move
        var bestScore = -1000
        var bestTokenId = validMoves.first()

        for (tokenId in validMoves) {
            val token = currentPlayer.tokens.firstOrNull { it.id == tokenId } ?: continue
            var score = 0

            if (token.state == TokenState.IN_HOME) {
                // Leaving home is very valuable
                score += 80
            } else {
                val targetIndex = token.positionIndex + diceVal
                if (targetIndex >= 0 && targetIndex < path.size) {
                    val targetBoardPos = path[targetIndex]

                    // Check if move lands on FINISHED (index 56)
                    if (targetIndex == 56) {
                        score += 150 // Finishing a token is top priority!
                    }

                    // Check if move reaches home column (safe from capture)
                    if (targetIndex >= 51 && token.positionIndex < 51) {
                        score += 70
                    }

                    // Check if move captures an opponent
                    if (!BoardConfig.safePositions.contains(targetBoardPos)) {
                        for (otherPlayer in state.players) {
                            if (otherPlayer.id == currentPlayer.id) continue
                            for (oppToken in otherPlayer.tokens) {
                                if (oppToken.state == TokenState.ON_BOARD && oppToken.boardPosition == targetBoardPos) {
                                    score += 120 // Huge reward for capturing!
                                }
                            }
                        }
                    }

                    // Check if target is a safe spot
                    if (BoardConfig.safePositions.contains(targetBoardPos)) {
                        score += 40
                    }

                    // Prefer advancing tokens that are further ahead
                    score += targetIndex
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestTokenId = tokenId
            }
        }

        return bestTokenId
    }
}

package com.example.ludo.engine

import com.example.ludo.model.TokenState
import kotlinx.coroutines.delay

/**
 * Intelligent AI opponent for Ludo.
 * Supports Easy (random choice) and Hard (heuristic scoring factoring in captures, home safety, token release, and advancement).
 */
class AIPlayer(private val engine: GameEngine, private val difficulty: String) {

    suspend fun executeRoll() {
        delay(550)
        engine.rollDice()
    }

    suspend fun executeMove(validMoves: List<Int>) {
        if (validMoves.isEmpty()) return
        delay(500)
        val chosenMove = chooseBestMove(validMoves)
        engine.selectToken(chosenMove)
    }

    private fun chooseBestMove(validMoves: List<Int>): Int {
        if (validMoves.size == 1) return validMoves.first()
        if (difficulty == "Easy") {
            return validMoves.random()
        }

        val state = engine.state.value
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return validMoves.first()
        val diceVal = state.diceResult?.value ?: 1
        val path = PathMapper.getPlayerPath(currentPlayer.id)

        var bestScore = Int.MIN_VALUE
        var bestTokenId = validMoves.first()

        for (tokenId in validMoves) {
            val token = currentPlayer.tokens.firstOrNull { it.id == tokenId } ?: continue
            var score = 0

            if (token.state == TokenState.IN_HOME) {
                score += 90 // High priority for deploying new token
            } else {
                val targetIndex = token.positionIndex + diceVal
                if (targetIndex in path.indices) {
                    val targetBoardPos = path[targetIndex]

                    // Finishing token into center goal
                    if (targetIndex == 56) {
                        score += 350
                    }

                    // Entering safe home column
                    if (targetIndex >= 51 && token.positionIndex < 51) {
                        score += 100
                    }

                    // Capturing opponent
                    if (!BoardConfig.safePositions.contains(targetBoardPos)) {
                        for (otherPlayer in state.players) {
                            if (otherPlayer.id == currentPlayer.id) continue
                            for (oppToken in otherPlayer.tokens) {
                                if (oppToken.state == TokenState.ON_BOARD && oppToken.boardPosition == targetBoardPos) {
                                    score += 200 // Maximum offensive reward
                                }
                            }
                        }
                    }

                    // Landing on safe zone
                    if (BoardConfig.safePositions.contains(targetBoardPos)) {
                        score += 50
                    }

                    // Forward progress bonus
                    score += targetIndex * 2
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


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
        delay(650)
        val chosenMove = chooseBestMove(validMoves)
        engine.selectToken(chosenMove)
    }

    private fun chooseBestMove(validMoves: List<Int>): Int {
        if (validMoves.size == 1) return validMoves.first()
        if (difficulty == "Easy") {
            return validMoves.random()
        }

        val state = engine.state.value
        val currentPlayer = state.players[state.currentPlayerIndex]
        val diceVal = state.diceResult?.value ?: 1
        val path = PathMapper.getPlayerPath(currentPlayer.id)

        var bestScore = -10000
        var bestTokenId = validMoves.first()

        for (tokenId in validMoves) {
            val token = currentPlayer.tokens.firstOrNull { it.id == tokenId } ?: continue
            var score = 0

            if (token.state == TokenState.IN_HOME) {
                score += 85 // Deploying a new token
            } else {
                val targetIndex = token.positionIndex + diceVal
                if (targetIndex >= 0 && targetIndex < path.size) {
                    val targetBoardPos = path[targetIndex]

                    // Finishing token
                    if (targetIndex == 56) {
                        score += 300
                    }

                    // Reaching safe home column
                    if (targetIndex >= 51 && token.positionIndex < 51) {
                        score += 90
                    }

                    // Capturing opponent
                    if (!BoardConfig.safePositions.contains(targetBoardPos)) {
                        for (otherPlayer in state.players) {
                            if (otherPlayer.id == currentPlayer.id) continue
                            for (oppToken in otherPlayer.tokens) {
                                if (oppToken.state == TokenState.ON_BOARD && oppToken.boardPosition == targetBoardPos) {
                                    score += 150 // Massive reward for capturing!
                                }
                            }
                        }
                    }

                    // Safe spot reward
                    if (BoardConfig.safePositions.contains(targetBoardPos)) {
                        score += 45
                    }

                    // Progress reward
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

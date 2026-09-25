package com.example.ludo.engine

import com.example.ludo.model.AiDifficulty
import com.example.ludo.model.GameState
import com.example.ludo.model.Player
import com.example.ludo.model.TokenState
import kotlin.random.Random

/**
 * Stateless AI move chooser. The engine owns timing; this only decides which token to move.
 * - EASY: uniformly random among valid moves.
 * - HARD: heuristic scoring of finishing, capturing, safety, threat avoidance and progress.
 */
class AIPlayer(
    private val difficulty: AiDifficulty,
    private val random: Random = Random.Default
) {

    fun chooseMove(state: GameState, validMoves: List<Int>): Int? {
        if (validMoves.isEmpty()) return null
        if (validMoves.size == 1) return validMoves.first()
        if (difficulty == AiDifficulty.EASY) return validMoves.random(random)

        val player = state.currentPlayer ?: return validMoves.first()
        val dice = state.diceResult?.value ?: return validMoves.first()
        return validMoves.maxByOrNull { scoreMove(player, it, dice, state.players) } ?: validMoves.first()
    }

    private fun scoreMove(player: Player, tokenId: Int, dice: Int, players: List<Player>): Int {
        val token = player.tokens.firstOrNull { it.id == tokenId } ?: return Int.MIN_VALUE
        val path = PathMapper.getPlayerPath(player.color.ordinal)

        if (token.state == TokenState.IN_HOME) {
            // Deploying is strong, but less so once several tokens are already out.
            val deployed = player.tokens.count { it.isOnMainTrack || it.state == TokenState.IN_HOME_COLUMN }
            return DEPLOY - deployed * 15
        }

        val target = token.positionIndex + dice
        val targetCell = path.getOrNull(target) ?: return Int.MIN_VALUE
        val onTrackAfter = target < BoardConfig.HOME_COLUMN_START
        var score = target // Mild preference for the most advanced token

        if (target == BoardConfig.GOAL_INDEX) score += FINISH
        if (!onTrackAfter && token.positionIndex < BoardConfig.HOME_COLUMN_START) score += ENTER_HOME_COLUMN

        if (onTrackAfter) {
            val safe = BoardConfig.isSafe(targetCell)
            if (safe) {
                score += SAFE_LANDING
            } else {
                // Capture: reward more for sending back a far-advanced opponent.
                for (other in players) {
                    if (other.id == player.id) continue
                    for (victim in other.tokens) {
                        if (victim.isOnMainTrack && victim.boardPosition == targetCell) {
                            score += CAPTURE + victim.positionIndex * 2
                        }
                    }
                }
                if (player.tokens.any { it.id != tokenId && it.isOnMainTrack && it.boardPosition == targetCell }) {
                    score += FORM_BLOCKADE
                }
                score -= threatsAt(targetCell, player, players) * THREAT_PENALTY
            }
        }

        // Escaping from a currently threatened, unsafe cell is worth more the further the token has come.
        if (token.isOnMainTrack && !BoardConfig.isSafe(token.boardPosition)) {
            val threatenedNow = threatsAt(token.boardPosition!!, player, players) > 0
            if (threatenedNow) score += ESCAPE + token.positionIndex
        }
        return score
    }

    companion object {
        private const val DEPLOY = 90
        private const val FINISH = 350
        private const val ENTER_HOME_COLUMN = 120
        private const val CAPTURE = 200
        private const val SAFE_LANDING = 50
        private const val FORM_BLOCKADE = 40
        private const val THREAT_PENALTY = 60
        private const val ESCAPE = 70

        /** Number of opponent tokens that could land on [cell] with a single roll of 1..6. */
        fun threatsAt(cell: Pair<Int, Int>, mover: Player, players: List<Player>): Int {
            val cellIdx = BoardConfig.trackIndexOf(cell)
            if (cellIdx < 0 || BoardConfig.isSafe(cell)) return 0
            var threats = 0
            for (other in players) {
                if (other.id == mover.id) continue
                for (t in other.tokens) {
                    if (!t.isOnMainTrack) continue
                    val oppIdx = BoardConfig.trackIndexOf(t.boardPosition)
                    if (oppIdx < 0) continue
                    val distance = (cellIdx - oppIdx + BoardConfig.TRACK_LENGTH) % BoardConfig.TRACK_LENGTH
                    // Must be reachable before the opponent turns into its own home column.
                    if (distance in 1..6 && t.positionIndex + distance < BoardConfig.HOME_COLUMN_START) threats++
                }
            }
            return threats
        }
    }
}

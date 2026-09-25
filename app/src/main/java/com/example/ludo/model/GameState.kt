package com.example.ludo.model

/**
 * A single on-board hop the UI should animate. The engine emits one per step and waits
 * [durationMs] before committing the move, so the UI owns frame interpolation and the
 * engine never pushes per-frame state.
 */
data class HopMove(
    val playerId: Int,
    val tokenId: Int,
    val from: Pair<Int, Int>,
    val to: Pair<Int, Int>,
    val durationMs: Int,
    val seq: Long
)

/** A one-shot celebration the board plays (a capture burst or a piece reaching home). */
data class BoardEffect(
    val kind: Kind,
    val color: PlayerColor,
    val cell: Pair<Int, Int>,
    val seq: Long
) {
    enum class Kind { CAPTURE, FINISH, SAFE }
}

data class GameState(
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.WAITING_FOR_ROLL,
    val diceResult: DiceResult? = null,
    val validMoves: List<Int> = emptyList(),
    val consecutiveSixes: Int = 0,
    val isGameOver: Boolean = false,
    val winnerId: Int? = null,
    val moveMessage: String = "",
    val isDiceRollingForPlayer: Int? = null,
    val hop: HopMove? = null,
    val effect: BoardEffect? = null
) {
    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)

    /** Players ordered by finishing rank (winner first); unranked players are omitted. */
    val standings: List<Player> get() = players.filter { it.rank > 0 }.sortedBy { it.rank }
}

enum class GamePhase {
    WAITING_FOR_ROLL,
    WAITING_FOR_MOVE,
    ANIMATING_MOVE,
    GAME_OVER
}

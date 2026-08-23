package com.example.ludo.model

data class GameState(
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.WAITING_FOR_ROLL,
    val diceResult: DiceResult? = null,
    val validMoves: List<Int> = emptyList(), // List of token IDs that can move
    val consecutiveSixes: Int = 0,
    val isGameOver: Boolean = false,
    val winnerId: Int? = null,
    val moveMessage: String = "" // For UI display like "Player 1 rolled 6"
)

enum class GamePhase {
    WAITING_FOR_ROLL,
    WAITING_FOR_MOVE,
    ANIMATING_MOVE,
    AI_THINKING,
    GAME_OVER
}

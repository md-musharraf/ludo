package com.example.ludo.model

data class CapturedTokenEvent(
    val playerId: Int,
    val tokenId: Int,
    val fromPosition: Pair<Int, Int>,
    val toHomePosition: Pair<Int, Int>,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameState(
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.WAITING_FOR_ROLL,
    val diceResult: DiceResult? = null,
    val validMoves: List<Int> = emptyList(), // List of token IDs that can move
    val consecutiveSixes: Int = 0,
    val isGameOver: Boolean = false,
    val winnerId: Int? = null,
    val moveMessage: String = "",
    val isDiceRollingForPlayer: Int? = null, // player id whose corner dice is currently rolling
    val animatingTokenId: Int? = null,
    val animatingPlayerId: Int? = null,
    val animatingFromPos: Pair<Int, Int>? = null,
    val animatingToPos: Pair<Int, Int>? = null,
    val animatingHopProgress: Float = 0f, // 0.0 to 1.0 for parabolic arc
    val lastCapturedEvent: CapturedTokenEvent? = null
)

enum class GamePhase {
    WAITING_FOR_ROLL,
    WAITING_FOR_MOVE,
    ANIMATING_MOVE,
    AI_THINKING,
    GAME_OVER
}

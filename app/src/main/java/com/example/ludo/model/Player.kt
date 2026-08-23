package com.example.ludo.model

data class Player(
    val id: Int, // 0: Red, 1: Green, 2: Yellow, 3: Blue
    val color: PlayerColor,
    val name: String,
    val isAI: Boolean,
    val tokens: List<Token> = emptyList(),
    val hasFinished: Boolean = false,
    val rank: Int = -1
)

enum class PlayerColor {
    RED, GREEN, YELLOW, BLUE
}

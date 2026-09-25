package com.example.ludo.model

data class Player(
    val id: Int, // 0: Red, 1: Green, 2: Yellow, 3: Blue
    val color: PlayerColor,
    val name: String,
    val isAI: Boolean,
    val tokens: List<Token> = emptyList(),
    val hasFinished: Boolean = false,
    val rank: Int = -1 // 1-based finishing position, -1 while still playing
)

enum class PlayerColor {
    RED, GREEN, YELLOW, BLUE
}

enum class AiDifficulty {
    EASY, HARD;

    companion object {
        /** Lenient parse for untrusted input (e.g. navigation arguments); defaults to HARD. */
        fun fromName(name: String?): AiDifficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: HARD
    }
}

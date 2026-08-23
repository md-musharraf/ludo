package com.example.ludo.model

data class Token(
    val id: Int, // 0 to 3 for each player
    val playerId: Int, // 0: Red, 1: Green, 2: Yellow, 3: Blue
    val state: TokenState = TokenState.IN_HOME,
    val positionIndex: Int = -1, // -1 when in home, 0-56 on board/home column
    val boardPosition: Pair<Int, Int>? = null // (row, col) on 15x15 grid
)

enum class TokenState {
    IN_HOME,
    ON_BOARD,
    IN_HOME_COLUMN,
    FINISHED
}

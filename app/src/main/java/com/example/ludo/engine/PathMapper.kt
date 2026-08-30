package com.example.ludo.engine

/**
 * High-performance precalculated path mapper for all 4 player colors.
 * Precomputes all 57 track coordinates per player once into static immutable lists,
 * eliminating heap allocations in 60-120 FPS render loops and AI evaluations.
 */
object PathMapper {
    private val PLAYER_PATHS: List<List<Pair<Int, Int>>> = List(4) { playerId ->
        val startIndex = when (playerId) {
            0 -> BoardConfig.RED_START_INDEX
            1 -> BoardConfig.GREEN_START_INDEX
            2 -> BoardConfig.YELLOW_START_INDEX
            3 -> BoardConfig.BLUE_START_INDEX
            else -> 0
        }

        val path = ArrayList<Pair<Int, Int>>(57)
        // 51 steps on the main track
        for (i in 0 until 51) {
            val trackIndex = (startIndex + i) % 52
            path.add(BoardConfig.mainTrack[trackIndex])
        }

        // 6 steps in home column
        val homeColumn = when (playerId) {
            0 -> BoardConfig.redHomeColumn
            1 -> BoardConfig.greenHomeColumn
            2 -> BoardConfig.yellowHomeColumn
            3 -> BoardConfig.blueHomeColumn
            else -> emptyList()
        }
        path.addAll(homeColumn)
        java.util.Collections.unmodifiableList(path)
    }

    /**
     * Returns the precomputed 57-step path for the given player (0: Red, 1: Green, 2: Yellow, 3: Blue).
     * O(1) complexity with 0 memory allocation.
     */
    fun getPlayerPath(playerId: Int): List<Pair<Int, Int>> {
        val safeIndex = playerId.coerceIn(0, 3)
        return PLAYER_PATHS[safeIndex]
    }
}


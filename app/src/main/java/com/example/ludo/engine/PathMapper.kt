package com.example.ludo.engine

/**
 * Precomputed 57-step path (51 shared track cells + 6 home-column cells) for each colour,
 * built once so render loops and AI evaluation never allocate.
 */
object PathMapper {
    private val PLAYER_PATHS: List<List<Pair<Int, Int>>> = List(4) { colorOrdinal ->
        val startIndex = BoardConfig.startIndices[colorOrdinal]
        val path = ArrayList<Pair<Int, Int>>(BoardConfig.GOAL_INDEX + 1)
        for (i in 0 until BoardConfig.HOME_COLUMN_START) {
            path.add(BoardConfig.mainTrack[(startIndex + i) % BoardConfig.TRACK_LENGTH])
        }
        path.addAll(BoardConfig.homeColumns[colorOrdinal])
        java.util.Collections.unmodifiableList(path)
    }

    /** Returns the cached path for a colour ordinal (0: Red, 1: Green, 2: Yellow, 3: Blue). */
    fun getPlayerPath(colorOrdinal: Int): List<Pair<Int, Int>> =
        PLAYER_PATHS[colorOrdinal.coerceIn(0, 3)]
}

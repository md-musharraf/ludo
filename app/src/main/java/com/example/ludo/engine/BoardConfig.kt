package com.example.ludo.engine

object BoardConfig {
    const val BOARD_SIZE = 15
    const val TRACK_LENGTH = 52

    /** Path index of the first home-column cell; indices below it are on the shared track. */
    const val HOME_COLUMN_START = 51

    /** Path index of the centre goal. */
    const val GOAL_INDEX = 56

    // 52 main track cells in clockwise order
    val mainTrack = listOf(
        Pair(6,1), Pair(6,2), Pair(6,3), Pair(6,4), Pair(6,5),
        Pair(5,6), Pair(4,6), Pair(3,6), Pair(2,6), Pair(1,6), Pair(0,6),
        Pair(0,7), Pair(0,8),
        Pair(1,8), Pair(2,8), Pair(3,8), Pair(4,8), Pair(5,8),
        Pair(6,9), Pair(6,10), Pair(6,11), Pair(6,12), Pair(6,13), Pair(6,14),
        Pair(7,14), Pair(8,14),
        Pair(8,13), Pair(8,12), Pair(8,11), Pair(8,10), Pair(8,9),
        Pair(9,8), Pair(10,8), Pair(11,8), Pair(12,8), Pair(13,8), Pair(14,8),
        Pair(14,7), Pair(14,6),
        Pair(13,6), Pair(12,6), Pair(11,6), Pair(10,6), Pair(9,6),
        Pair(8,5), Pair(8,4), Pair(8,3), Pair(8,2), Pair(8,1), Pair(8,0),
        Pair(7,0), Pair(6,0)
    )

    const val RED_START_INDEX = 0
    const val GREEN_START_INDEX = 13
    const val YELLOW_START_INDEX = 26
    const val BLUE_START_INDEX = 39

    /** Track start index per colour ordinal (Red, Green, Yellow, Blue). */
    val startIndices = intArrayOf(RED_START_INDEX, GREEN_START_INDEX, YELLOW_START_INDEX, BLUE_START_INDEX)

    val redHomeColumn = listOf(Pair(7,1), Pair(7,2), Pair(7,3), Pair(7,4), Pair(7,5), Pair(7,6))
    val greenHomeColumn = listOf(Pair(1,7), Pair(2,7), Pair(3,7), Pair(4,7), Pair(5,7), Pair(6,7))
    val yellowHomeColumn = listOf(Pair(7,13), Pair(7,12), Pair(7,11), Pair(7,10), Pair(7,9), Pair(7,8))
    val blueHomeColumn = listOf(Pair(13,7), Pair(12,7), Pair(11,7), Pair(10,7), Pair(9,7), Pair(8,7))

    /** Home column per colour ordinal (Red, Green, Yellow, Blue). */
    val homeColumns = listOf(redHomeColumn, greenHomeColumn, yellowHomeColumn, blueHomeColumn)

    val safeSpotsIndices = listOf(0, 8, 13, 21, 26, 34, 39, 47)
    val starSpotIndices = safeSpotsIndices.filterNot { it in startIndices }

    /** Set for O(1) membership checks in move validation, AI scoring and rendering. */
    val safePositions: Set<Pair<Int, Int>> = safeSpotsIndices.mapTo(HashSet()) { mainTrack[it] }

    private val trackIndexByCell: Map<Pair<Int, Int>, Int> =
        mainTrack.withIndex().associate { (i, cell) -> cell to i }

    /** Index of [cell] on the shared 52-cell loop, or -1 when it is not a track cell. */
    fun trackIndexOf(cell: Pair<Int, Int>?): Int = cell?.let { trackIndexByCell[it] } ?: -1

    fun isSafe(cell: Pair<Int, Int>?): Boolean = cell != null && cell in safePositions

    /** The star-marked safe squares (the coloured start squares are safe too, but unmarked). */
    val starPositions: Set<Pair<Int, Int>> = starSpotIndices.mapTo(HashSet()) { mainTrack[it] }

    fun isStar(cell: Pair<Int, Int>?): Boolean = cell != null && cell in starPositions

    val homePositions = mapOf(
        0 to listOf(Pair(1, 1), Pair(1, 4), Pair(4, 1), Pair(4, 4)),
        1 to listOf(Pair(1, 10), Pair(1, 13), Pair(4, 10), Pair(4, 13)),
        2 to listOf(Pair(10, 10), Pair(10, 13), Pair(13, 10), Pair(13, 13)),
        3 to listOf(Pair(10, 1), Pair(10, 4), Pair(13, 1), Pair(13, 4))
    )

    fun homeSpot(colorOrdinal: Int, tokenId: Int): Pair<Int, Int> =
        homePositions[colorOrdinal]?.getOrNull(tokenId) ?: Pair(0, 0)
}

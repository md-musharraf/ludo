package com.example.ludo.engine

data class BoardPortal(
    val fromTrackIndex: Int,
    val toTrackIndex: Int,
    val fromPos: Pair<Int, Int>,
    val toPos: Pair<Int, Int>
)

object BoardConfig {
    val BOARD_SIZE = 15

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

    val RED_START_INDEX = 0
    val GREEN_START_INDEX = 13
    val YELLOW_START_INDEX = 26
    val BLUE_START_INDEX = 39

    val redHomeColumn = listOf(Pair(7,1), Pair(7,2), Pair(7,3), Pair(7,4), Pair(7,5), Pair(7,6))
    val greenHomeColumn = listOf(Pair(1,7), Pair(2,7), Pair(3,7), Pair(4,7), Pair(5,7), Pair(6,7))
    val yellowHomeColumn = listOf(Pair(7,13), Pair(7,12), Pair(7,11), Pair(7,10), Pair(7,9), Pair(7,8))
    val blueHomeColumn = listOf(Pair(13,7), Pair(12,7), Pair(11,7), Pair(10,7), Pair(9,7), Pair(8,7))

    val safeSpotsIndices = listOf(0, 8, 13, 21, 26, 34, 39, 47)
    val safePositions = safeSpotsIndices.map { mainTrack[it] }

    val homePositions = mapOf(
        0 to listOf(Pair(2,2), Pair(2,3), Pair(3,2), Pair(3,3)),
        1 to listOf(Pair(2,11), Pair(2,12), Pair(3,11), Pair(3,12)),
        2 to listOf(Pair(11,11), Pair(11,12), Pair(12,11), Pair(12,12)),
        3 to listOf(Pair(11,2), Pair(11,3), Pair(12,2), Pair(12,3))
    )

    // 4 Symmetrical Ladders (Base -> Top, +8 track step boost)
    val ladders: List<BoardPortal> = listOf(
        BoardPortal(4, 12, mainTrack[4], mainTrack[12]),
        BoardPortal(17, 25, mainTrack[17], mainTrack[25]),
        BoardPortal(30, 38, mainTrack[30], mainTrack[38]),
        BoardPortal(43, 51, mainTrack[43], mainTrack[51])
    )

    // 4 Symmetrical Snakes (Head -> Tail, -8 track step drop)
    val snakes: List<BoardPortal> = listOf(
        BoardPortal(11, 3, mainTrack[11], mainTrack[3]),
        BoardPortal(24, 16, mainTrack[24], mainTrack[16]),
        BoardPortal(37, 29, mainTrack[37], mainTrack[29]),
        BoardPortal(50, 42, mainTrack[50], mainTrack[42])
    )
}

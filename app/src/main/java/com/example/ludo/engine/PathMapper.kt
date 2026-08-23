package com.example.ludo.engine

object PathMapper {
    fun getPlayerPath(playerId: Int): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        val startIndex = when(playerId) {
            0 -> BoardConfig.RED_START_INDEX
            1 -> BoardConfig.GREEN_START_INDEX
            2 -> BoardConfig.YELLOW_START_INDEX
            3 -> BoardConfig.BLUE_START_INDEX
            else -> 0
        }
        
        // 51 steps on the main track
        for (i in 0 until 51) {
            val trackIndex = (startIndex + i) % 52
            path.add(BoardConfig.mainTrack[trackIndex])
        }
        
        // 6 steps in home column
        val homeColumn = when(playerId) {
            0 -> BoardConfig.redHomeColumn
            1 -> BoardConfig.greenHomeColumn
            2 -> BoardConfig.yellowHomeColumn
            3 -> BoardConfig.blueHomeColumn
            else -> emptyList()
        }
        path.addAll(homeColumn)
        
        return path
    }
}

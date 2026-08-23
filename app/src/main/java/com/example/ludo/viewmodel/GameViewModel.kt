package com.example.ludo.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ludo.engine.GameEngine
import com.example.ludo.model.GameState
import kotlinx.coroutines.flow.StateFlow

class GameViewModel(
    private val playerCount: Int,
    private val isVsAI: Boolean,
    private val aiDifficulty: String
) : ViewModel() {
    private val engine = GameEngine()
    val gameState: StateFlow<GameState> = engine.state

    init {
        engine.resetGame(playerCount, isVsAI, aiDifficulty)
    }

    fun rollDice() {
        engine.rollDice()
    }

    fun selectToken(tokenId: Int) {
        engine.selectToken(tokenId)
    }

    fun resetGame() {
        engine.resetGame(playerCount, isVsAI, aiDifficulty)
    }
}

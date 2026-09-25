package com.example.ludo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.engine.GameEngine
import com.example.ludo.model.AiDifficulty
import com.example.ludo.model.GameState
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the [GameEngine] for one match. The engine runs on [viewModelScope] (main thread), so it
 * survives configuration changes and all of its coroutines stop when the screen is left.
 */
class GameViewModel(
    playerCount: Int,
    private val isVsAI: Boolean,
    private val aiDifficulty: AiDifficulty
) : ViewModel() {
    private val playerCount = playerCount.coerceIn(2, 4)
    private val engine = GameEngine(viewModelScope)
    val gameState: StateFlow<GameState> = engine.state

    init {
        resetGame()
    }

    fun rollDice() = engine.rollDice()

    fun selectToken(tokenId: Int) = engine.selectToken(tokenId)

    fun resetGame() = engine.resetGame(playerCount, isVsAI, aiDifficulty)
}

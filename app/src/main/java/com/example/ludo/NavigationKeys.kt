package com.example.ludo

import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Home

@Serializable
data class Game(val playerCount: Int, val isVsAI: Boolean, val aiDifficulty: String)

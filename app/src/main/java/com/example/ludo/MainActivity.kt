package com.example.ludo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.core.logging.AppLogger
import com.example.ludo.theme.LudoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i("MainActivity") { "MainActivity onCreate" }
        enableEdgeToEdge()
        setContent {
            LudoTheme {
                Surface {
                    MainNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i("MainActivity") { "Cleaning up resources on onDestroy" }
        SoundEffectManager.release()
    }
}

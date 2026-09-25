package com.example.ludo

import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.core.logging.AppLogger
import com.example.ludo.theme.LudoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Logging only in debuggable builds; release builds stay silent.
        AppLogger.isLoggingEnabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AppLogger.i("MainActivity") { "MainActivity onCreate" }
        // The UI is always light, so force dark system-bar icons even when the phone is in dark mode.
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
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
        // Keep cached audio across rotation; free native tracks only when truly leaving.
        if (isFinishing) {
            AppLogger.i("MainActivity") { "Releasing audio resources" }
            SoundEffectManager.release()
        }
    }
}

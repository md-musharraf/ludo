package com.example.ludo.core

import com.example.ludo.core.logging.AppLogger
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.LudoBlue
import com.example.ludo.theme.LudoGreen
import com.example.ludo.theme.LudoRed
import com.example.ludo.theme.LudoYellow
import org.junit.Assert.*
import org.junit.Test

class CoreUtilsTest {

    @Test
    fun testPlayerColorUtilsComposeColors() {
        assertEquals(LudoRed, PlayerColorUtils.getComposeColor(PlayerColor.RED))
        assertEquals(LudoGreen, PlayerColorUtils.getComposeColor(PlayerColor.GREEN))
        assertEquals(LudoYellow, PlayerColorUtils.getComposeColor(PlayerColor.YELLOW))
        assertEquals(LudoBlue, PlayerColorUtils.getComposeColor(PlayerColor.BLUE))
        assertEquals(LudoGreen, PlayerColorUtils.getComposeColor(null))
    }

    @Test
    fun testPlayerColorUtilsPawnColorSchemes() {
        val redScheme = PlayerColorUtils.getPawnColorScheme(PlayerColor.RED)
        assertNotNull(redScheme)
        assertEquals(PlayerColorUtils.RedPawnScheme, redScheme)

        val greenScheme = PlayerColorUtils.getPawnColorScheme(PlayerColor.GREEN)
        assertNotNull(greenScheme)
        assertEquals(PlayerColorUtils.GreenPawnScheme, greenScheme)

        val yellowScheme = PlayerColorUtils.getPawnColorScheme(PlayerColor.YELLOW)
        assertNotNull(yellowScheme)
        assertEquals(PlayerColorUtils.YellowPawnScheme, yellowScheme)

        val blueScheme = PlayerColorUtils.getPawnColorScheme(PlayerColor.BLUE)
        assertNotNull(blueScheme)
        assertEquals(PlayerColorUtils.BluePawnScheme, blueScheme)
    }

    @Test
    fun testAppLoggerMeasureTrace() {
        var executed = false
        val result = AppLogger.measureTrace("TestTrace") {
            executed = true
            42
        }
        assertTrue("Trace block must execute", executed)
        assertEquals(42, result)
    }

    @Test
    fun testAppLoggerSafeLogging() {
        AppLogger.d("UnitTest") { "Debug message" }
        AppLogger.i("UnitTest") { "Info message" }
        AppLogger.w("UnitTest", RuntimeException("Warn test")) { "Warn message" }
        AppLogger.e("UnitTest", RuntimeException("Error test")) { "Error message" }
        // Passes without throwing exceptions
    }
}

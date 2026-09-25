package com.example.ludo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.core.settings.AppSettings
import com.example.ludo.model.AiDifficulty
import com.example.ludo.theme.*
import com.example.ludo.ui.components.*

private enum class Opponents(val label: String) { COMPUTER("Computer"), LOCAL("Pass & Play") }

@Composable
fun HomeScreen(onStartGame: (Int, Boolean, AiDifficulty) -> Unit) {
    // Saveable so choices survive rotation and returning from a match.
    var playerCount by rememberSaveable { mutableIntStateOf(4) }
    var opponents by rememberSaveable { mutableStateOf(Opponents.COMPUTER) }
    var aiDifficulty by rememberSaveable { mutableStateOf(AiDifficulty.HARD) }
    var showRules by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(AppSettings.soundEnabled) }

    val tap = { SoundEffectManager.playButtonTap() }
    val start = {
        tap()
        onStartGame(playerCount, opponents == Opponents.COMPUTER, aiDifficulty)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val wide = maxWidth >= 720.dp
        val compact = maxHeight < 640.dp

        val brand = @Composable { modifier: Modifier ->
            Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                LogoMark(size = if (compact) 64.dp else 84.dp)
                Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
                LudoTitle(fontSize = if (compact) 40.sp else 52.sp)
                Text("The classic board game", fontSize = 14.sp, color = InkMuted, fontWeight = FontWeight.Medium)
            }
        }

        val setup = @Composable { modifier: Modifier ->
            Column(modifier) {
                LudoCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("New Game", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkDark)
                        Spacer(Modifier.height(16.dp))

                        SectionLabel("Players")
                        Spacer(Modifier.height(8.dp))
                        SegmentedControl(
                            options = listOf(2, 3, 4),
                            selected = playerCount,
                            label = { "$it" },
                            onSelect = { tap(); playerCount = it }
                        )

                        Spacer(Modifier.height(16.dp))
                        SectionLabel("Opponents")
                        Spacer(Modifier.height(8.dp))
                        SegmentedControl(
                            options = Opponents.entries,
                            selected = opponents,
                            label = { it.label },
                            onSelect = { tap(); opponents = it }
                        )

                        AnimatedVisibility(
                            visible = opponents == Opponents.COMPUTER,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                SectionLabel("Difficulty")
                                Spacer(Modifier.height(8.dp))
                                SegmentedControl(
                                    options = AiDifficulty.entries,
                                    selected = aiDifficulty,
                                    label = { if (it == AiDifficulty.EASY) "Easy" else "Hard" },
                                    onSelect = { tap(); aiDifficulty = it },
                                    accent = if (aiDifficulty == AiDifficulty.EASY) LudoGreen else LudoRed
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                PrimaryButton("Play", onClick = start, icon = Icons.Rounded.PlayArrow, height = if (compact) 52.dp else 58.dp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Rules", onClick = { tap(); showRules = true }, icon = Icons.Rounded.Info, modifier = Modifier.weight(1f))
                    SecondaryButton("Settings", onClick = { tap(); showSettings = true }, icon = Icons.Rounded.Settings, modifier = Modifier.weight(1f))
                }
            }
        }

        if (wide) {
            Row(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                brand(Modifier.weight(1f))
                setup(Modifier.weight(1f).widthIn(max = 460.dp))
            }
        } else {
            // Scrolls on short screens, stays centred on tall ones, capped width on tablets.
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                brand(Modifier)
                Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
                setup(Modifier.fillMaxWidth())
            }
        }
    }

    if (showRules) RulesDialog(onDismiss = { showRules = false })
    if (showSettings) {
        SettingsDialog(
            soundEnabled = soundEnabled,
            onSoundChange = {
                soundEnabled = it
                AppSettings.soundEnabled = it
            },
            onDismiss = { showSettings = false }
        )
    }
}

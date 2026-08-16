package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.BasketballCourtCanvas
import com.example.ui.components.ScoreOverlay
import com.example.ui.viewmodel.BasketballViewModel

@Composable
fun SoloGameScreen(
    viewModel: BasketballViewModel,
    onFinishGame: () -> Unit,
    onExitToHome: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsState()
    val gameMode by viewModel.soloGameMode.collectAsState()
    val currentZone by viewModel.currentShotZone.collectAsState()
    val score by viewModel.soloScore.collectAsState()
    val streak by viewModel.soloStreak.collectAsState()
    val timerSeconds by viewModel.soloTimerSeconds.collectAsState()
    val shotsTaken by viewModel.totalShotsTaken.collectAsState()
    val shotsScored by viewModel.totalShotsScored.collectAsState()
    val isPaused by viewModel.isGamePaused.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val hoopSpeed by viewModel.hoopSpeed.collectAsState()

    val ballSkin = PRESET_BALL_SKINS.find { it.id == profile.ballSkinId } ?: PRESET_BALL_SKINS.first()
    val playerAvatar = PRESET_AVATARS.find { it.id == profile.avatarId } ?: PRESET_AVATARS.first()

    // When game over triggers, navigate to match result
    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            onFinishGame()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Main Court Canvas with Physics & Shooting
        BasketballCourtCanvas(
            modifier = Modifier.fillMaxSize(),
            gameMode = gameMode,
            shotZone = currentZone,
            ballSkin = ballSkin,
            playerAvatar = playerAvatar,
            jerseyNumber = profile.jerseyNumber,
            playerName = profile.name,
            isMovingHoop = (gameMode == GameMode.SOLO_MOVING_HOOP),
            hoopSpeedFactor = hoopSpeed.speedFactor,
            isPaused = isPaused,
            onScore = { points, isSwish, isBank ->
                viewModel.onScoreMade(points, isSwish, isBank)
            },
            onShotTaken = {
                viewModel.onShotTaken()
            },
            onSoundEvent = { soundType ->
                viewModel.playSoundEffect(soundType)
            }
        )

        // Top Status Overlay (Score, Timer, Streaks, 2PT/3PT selector, Stop & Pause/Resume)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            ScoreOverlay(
                score = score,
                streak = streak,
                timerSeconds = timerSeconds,
                gameMode = gameMode,
                currentZone = currentZone,
                shotsTaken = shotsTaken,
                shotsScored = shotsScored,
                isPaused = isPaused,
                onZoneChanged = { viewModel.setShotZone(it) },
                onPauseClicked = { viewModel.togglePause() },
                onStopClicked = { viewModel.finishSoloGame() }
            )
        }

        // Bottom Aiming Guide & Quick Action Control Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hint text & Creator attribution
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "💡 Tarik bebas & atur sudut busur sesukamu!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Created By : Cikgu Dasril Amri",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B).copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons: Stop & Pause / Resume
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Stop Permainan
                Button(
                    onClick = { viewModel.finishSoloGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("finish_solo_game_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stop",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Tombol Lanjut / Jeda
                Button(
                    onClick = { viewModel.togglePause() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFF10B981) else Color(0xFF0284C7).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPaused) "Lanjut" else "Jeda",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Pause Modal Dialog
        if (isPaused) {
            Dialog(onDismissRequest = { viewModel.togglePause() }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PERMAINAN DIJEDA",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Skor Sementara: $score Poin",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Resume Button
                        Button(
                            onClick = { viewModel.togglePause() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Lanjutkan Permainan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stop Game Button
                        Button(
                            onClick = {
                                viewModel.togglePause()
                                viewModel.finishSoloGame()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Hentikan Permainan (Stop)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Restart Button
                        OutlinedButton(
                            onClick = {
                                viewModel.startSoloGame(gameMode, hoopSpeed)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Mulai Ulang Dari Awal", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Exit to home
                        TextButton(
                            onClick = {
                                viewModel.finishSoloGame()
                                onExitToHome()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Keluar ke Menu Utama", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

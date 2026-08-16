package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.BasketballCourtCanvas
import com.example.ui.components.ScoreOverlay
import com.example.ui.viewmodel.BasketballViewModel

@Composable
fun GroupPlayerGameScreen(
    viewModel: BasketballViewModel,
    onMatchFinished: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsState()
    val activeRoom by viewModel.activeTournamentRoom.collectAsState()
    val players by viewModel.tournamentPlayers.collectAsState()

    val currentZone by viewModel.currentShotZone.collectAsState()
    val ballSkin = PRESET_BALL_SKINS.find { it.id == profile.ballSkinId } ?: PRESET_BALL_SKINS.first()
    val playerAvatar = PRESET_AVATARS.find { it.id == profile.avatarId } ?: PRESET_AVATARS.first()

    // Find current user player record in the live tournament
    val myPlayerRecord = players.find { it.id == profile.id }
    val myScore = myPlayerRecord?.score ?: 0
    val myRank = myPlayerRecord?.rank ?: 1

    var localStreak by remember { mutableIntStateOf(0) }
    var shotsTaken by remember { mutableIntStateOf(0) }
    var shotsScored by remember { mutableIntStateOf(0) }

    // When admin finishes match or timer hits 0, navigate to result
    LaunchedEffect(activeRoom?.isFinished) {
        if (activeRoom?.isFinished == true) {
            onMatchFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Main Court Canvas
        BasketballCourtCanvas(
            modifier = Modifier.fillMaxSize(),
            gameMode = GameMode.GROUP_TOURNAMENT,
            shotZone = currentZone,
            ballSkin = ballSkin,
            playerAvatar = playerAvatar,
            jerseyNumber = profile.jerseyNumber,
            playerName = profile.name,
            isMovingHoop = (activeRoom?.hoopSpeed != "Diam"),
            hoopSpeedFactor = if (activeRoom?.hoopSpeed == "Cepat") 2.4f else 1.5f,
            isPaused = (activeRoom?.isPaused == true),
            onScore = { points, isSwish, isBank ->
                shotsScored++
                localStreak++
                viewModel.submitTournamentShot(points = points, isGoal = true, streak = localStreak)
                if (isSwish) viewModel.playSoundEffect("swish") else viewModel.playSoundEffect("score")
            },
            onShotTaken = {
                shotsTaken++
                viewModel.submitTournamentShot(points = 0, isGoal = false, streak = localStreak)
            },
            onSoundEvent = { type ->
                viewModel.playSoundEffect(type)
            }
        )

        // Top Status Overlay (Timer, Score, Streaks, 2PT/3PT selector)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            ScoreOverlay(
                score = myScore,
                streak = localStreak,
                timerSeconds = activeRoom?.remainingSeconds,
                gameMode = GameMode.GROUP_TOURNAMENT,
                currentZone = currentZone,
                shotsTaken = shotsTaken,
                shotsScored = shotsScored,
                onZoneChanged = { viewModel.setShotZone(it) },
                onPauseClicked = { /* Admin controlled */ }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mini Live Leaderboard Strip (Shows My Rank and Top Competitors)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // My Rank Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (myRank == 1) Color(0xFFF59E0B) else Color(0xFF0284C7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "POSISI #$myRank",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${players.size} Pemain Bersaing",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Leader preview
                val leader = players.firstOrNull()
                if (leader != null) {
                    Text(
                        text = "👑 ${leader.playerName}: ${leader.score} pt",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Bottom Info Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🏀 Tarik Bebas | Room: ${activeRoom?.roomCode}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF38BDF8)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B4B).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Created By : Cikgu Dasril Amri",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFEF08A)
                )
            }
        }

        // Pause Overlay if admin paused
        if (activeRoom?.isPaused == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.PauseCircle, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "PERMAINAN DIJEDA ADMIN", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "Menunggu admin melanjutkan laga...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

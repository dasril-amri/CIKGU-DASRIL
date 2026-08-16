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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.ui.components.LeaderboardPodium
import com.example.ui.components.PlayerLeaderboardList
import com.example.ui.viewmodel.BasketballViewModel

@Composable
fun MatchResultScreen(
    viewModel: BasketballViewModel,
    isGroupTournament: Boolean,
    onPlayAgain: () -> Unit,
    onBackToHome: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsState()
    val soloScore by viewModel.soloScore.collectAsState()
    val highestStreak by viewModel.highestStreakInMatch.collectAsState()
    val totalTaken by viewModel.totalShotsTaken.collectAsState()
    val totalScored by viewModel.totalShotsScored.collectAsState()
    val twoPoints by viewModel.twoPointsCount.collectAsState()
    val threePoints by viewModel.threePointsCount.collectAsState()
    val swishCount by viewModel.swishCount.collectAsState()
    val gameMode by viewModel.soloGameMode.collectAsState()

    val tournamentPlayers by viewModel.tournamentPlayers.collectAsState()
    val activeRoom by viewModel.activeTournamentRoom.collectAsState()

    val myTournamentRecord = tournamentPlayers.find { it.id == profile.id }
    val finalScore = if (isGroupTournament) myTournamentRecord?.score ?: 0 else soloScore
    val finalRank = myTournamentRecord?.rank ?: 1
    val accuracy = if (totalTaken > 0) ((totalScored.toFloat() / totalTaken) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF090D16))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Trophy / Medallion Header
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                    )
                )
                .border(3.dp, Color(0xFFFEF08A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PERTANDINGAN SELESAI!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = Color.White
        )
        Text(
            text = if (isGroupTournament) "Hasil Akhir Turnamen: ${activeRoom?.roomName}" else "Mode: ${gameMode.displayName}",
            fontSize = 13.sp,
            color = Color(0xFF38BDF8)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // GROUP TOURNAMENT PODIUM & RANKINGS
        if (isGroupTournament && tournamentPlayers.isNotEmpty()) {
            Text(
                text = "PODIUM JUARA 🏆",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF59E0B),
                letterSpacing = 1.sp
            )

            LeaderboardPodium(players = tournamentPlayers)

            Spacer(modifier = Modifier.height(16.dp))

            // My Placement Highlight
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0369A1).copy(alpha = 0.35f)),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Peringkat Akhir Anda",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = when (finalRank) {
                                1 -> "🥇 Juara 1 (Champion!)"
                                2 -> "🥈 Juara 2 (Runner Up)"
                                3 -> "🥉 Juara 3 (Bronze)"
                                else -> "Posisi #$finalRank dari ${tournamentPlayers.size}"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (finalRank == 1) Color(0xFFF59E0B) else Color.White
                        )
                    }

                    Text(
                        text = "$finalScore Poin",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Papan Peringkat Lengkap",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            PlayerLeaderboardList(
                players = tournamentPlayers,
                currentUserId = profile.id,
                modifier = Modifier.heightIn(max = 240.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        } else {
            // SOLO SCORE SUMMARY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, Color(0xFFEA580C).copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL SKOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$finalScore",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = "Poin",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Grid Statistics: 2PT, 3PT, Akurasi, Swish, Streak
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "2 Poin", value = "$twoPoints", color = Color(0xFFF59E0B))
                        StatItem(label = "3 Poin", value = "$threePoints", color = Color(0xFF38BDF8))
                        StatItem(label = "Akurasi", value = "$accuracy%", color = Color(0xFF10B981))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "Swish Bersih", value = "$swishCount", color = Color(0xFF818CF8))
                        StatItem(label = "Best Streak", value = "🔥 $highestStreak", color = Color(0xFFEF4444))
                        StatItem(label = "Total Tembakan", value = "$totalTaken", color = Color(0xFF94A3B8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Action Buttons: Main Lagi & Menu Utama
        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("play_again_btn")
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Replay, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Main Lagi",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("back_to_home_btn")
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF64748B))
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Kembali ke Menu Utama",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

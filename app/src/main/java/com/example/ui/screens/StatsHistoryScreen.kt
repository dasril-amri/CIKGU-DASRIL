package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.MatchRecord
import com.example.ui.viewmodel.BasketballViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsHistoryScreen(
    viewModel: BasketballViewModel,
    onBackToHome: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsState()
    val matchRecords by viewModel.matchRecords.collectAsState()

    val overallAccuracy = if (profile.totalShots > 0) {
        ((profile.totalGoals.toFloat() / profile.totalShots) * 100).toInt()
    } else 0

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

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
            .padding(20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Statistik & Riwayat Laga",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Catatan Rekor Menembak Bola Basket",
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // CAREER HIGHLIGHTS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PENCAPAIAN KARIR 🌟",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CareerStatBox(title = "Skor Tertinggi", value = "${profile.highestScoreSolo}", color = Color(0xFFF59E0B))
                    CareerStatBox(title = "Total Poin", value = "${profile.totalScore}", color = Color(0xFF38BDF8))
                    CareerStatBox(title = "Akurasi Total", value = "$overallAccuracy%", color = Color(0xFF10B981))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CareerStatBox(title = "Best Streak", value = "🔥 ${profile.highestStreak}", color = Color(0xFFEF4444))
                    CareerStatBox(title = "Total Gol", value = "${profile.totalGoals}", color = Color(0xFF818CF8))
                    CareerStatBox(title = "Laga Dimainkan", value = "${profile.matchesPlayed}", color = Color(0xFFCBD5E1))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // MATCH HISTORY LIST
        Text(
            text = "Riwayat Pertandingan Terakhir (${matchRecords.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (matchRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Belum ada riwayat laga", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text(text = "Mainkan mode Solo atau Turnamen Kelompok untuk mencatat skor!", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matchRecords) { record ->
                    MatchRecordItem(record = record, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun CareerStatBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun MatchRecordItem(record: MatchRecord, dateFormat: SimpleDateFormat) {
    val dateStr = try {
        dateFormat.format(Date(record.timestamp))
    } catch (_: Exception) {
        "-"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.gameMode,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2PT: ${record.twoPointsScored} | 3PT: ${record.threePointsScored} | Akurasi: ${record.accuracyPercent.toInt()}%",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${record.score}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "POIN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

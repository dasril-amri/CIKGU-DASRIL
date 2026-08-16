package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.ShotZone

@Composable
fun ScoreOverlay(
    score: Int,
    streak: Int,
    timerSeconds: Int?,
    gameMode: GameMode,
    currentZone: ShotZone,
    shotsTaken: Int,
    shotsScored: Int,
    isPaused: Boolean = false,
    onZoneChanged: (ShotZone) -> Unit,
    onPauseClicked: () -> Unit,
    onStopClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accuracy = if (shotsTaken > 0) ((shotsScored.toFloat() / shotsTaken) * 100).toInt() else 0

    // Controls whether the 2PT/3PT selector options are expanded or hidden
    var showZonePicker by remember { mutableStateOf(false) }

    // Pulse animation for high streaks
    val infiniteTransition = rememberInfiniteTransition(label = "streakPulse")
    val streakScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (streak >= 3) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Status Bar: Score, Timer/Mode, Streak, Stop & Pause/Resume
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Score Box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E293B).copy(alpha = 0.9f), Color(0xFF0F172A).copy(alpha = 0.9f))
                        )
                    )
                    .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsBasketball,
                    contentDescription = "Score",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "SKOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$score",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Middle: Timer or Mode Title
            if (timerSeconds != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (timerSeconds <= 10) Color(0xFFDC2626).copy(alpha = 0.9f)
                            else Color(0xFF1E293B).copy(alpha = 0.9f)
                        )
                        .border(
                            1.5.dp,
                            if (timerSeconds <= 10) Color(0xFFF87171) else Color(0xFF64748B),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (timerSeconds <= 10) Color.White else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${timerSeconds}s",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = gameMode.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            // Right side: Streak & Action Buttons (Stop & Pause/Resume)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (streak > 0) {
                    Box(
                        modifier = Modifier
                            .scale(streakScale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    if (streak >= 3) listOf(Color(0xFFEA580C), Color(0xFFDC2626))
                                    else listOf(Color(0xFF0D9488), Color(0xFF059669))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (streak >= 3) "🔥 x$streak" else "⚡ x$streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Tombol Stop / Selesai
                if (onStopClicked != null) {
                    IconButton(
                        onClick = onStopClicked,
                        modifier = Modifier
                            .testTag("stop_game_button")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626).copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Permainan",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Tombol Jeda (Pause) atau Lanjut (Resume)
                Button(
                    onClick = onPauseClicked,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFF10B981) else Color(0xFF1E293B).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPaused) Color(0xFF34D399) else Color(0xFF475569)
                    ),
                    modifier = Modifier
                        .testTag("pause_resume_button")
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Lanjut" else "Jeda",
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

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Info Bar: Accuracy & Collapsible Zone Selector (2PT / 3PT)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accuracy & Shot Counter Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Akurasi: $accuracy% ($shotsScored/$shotsTaken)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }

            // Compact Active Mode Pill (Tapping expands options; selection collapses it)
            Surface(
                onClick = { showZonePicker = !showZonePicker },
                shape = RoundedCornerShape(16.dp),
                color = if (currentZone.points == 3) Color(0xFF0284C7).copy(alpha = 0.85f) else Color(0xFFD97706).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("toggle_zone_picker_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎯 ${currentZone.points}PT: ${when (currentZone) {
                            ShotZone.TWO_POINT_CLOSE -> "Dekat"
                            ShotZone.TWO_POINT_MID -> "Mid-Range"
                            ShotZone.THREE_POINT -> "Garis 3PT"
                            ShotZone.DEEP_THREE -> "Deep 3PT"
                        }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showZonePicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Pilih Mode",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Expanded Zone Options (Hidden automatically when an option is selected)
        AnimatedVisibility(
            visible = showZonePicker,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PILIH MODE / JARAK TEMBAKAN:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    IconButton(
                        onClick = { showZonePicker = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShotZone.values().forEach { zone ->
                        val isSelected = currentZone == zone
                        val zoneColor = if (zone.points == 3) Color(0xFF0284C7) else Color(0xFFD97706)

                        Surface(
                            onClick = {
                                onZoneChanged(zone)
                                // Automatically hide selection once chosen!
                                showZonePicker = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) zoneColor else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.White else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("zone_button_${zone.name}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "${zone.points} Poin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = when (zone) {
                                        ShotZone.TWO_POINT_CLOSE -> "Dekat"
                                        ShotZone.TWO_POINT_MID -> "Mid"
                                        ShotZone.THREE_POINT -> "3PT"
                                        ShotZone.DEEP_THREE -> "Deep"
                                    },
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color(0xFFFEF08A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

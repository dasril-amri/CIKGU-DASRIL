package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RoomPlayer
import com.example.model.PRESET_AVATARS

@Composable
fun LeaderboardPodium(
    players: List<RoomPlayer>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return

    val top3 = players.take(3)
    val first = top3.getOrNull(0)
    val second = top3.getOrNull(1)
    val third = top3.getOrNull(2)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        if (second != null) {
            PodiumColumn(
                player = second,
                rank = 2,
                podiumHeight = 90.dp,
                badgeColor = Color(0xFF94A3B8),
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1st Place (Gold - Taller in center)
        if (first != null) {
            PodiumColumn(
                player = first,
                rank = 1,
                podiumHeight = 120.dp,
                badgeColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1.15f)
            )
        }

        // 3rd Place (Bronze)
        if (third != null) {
            PodiumColumn(
                player = third,
                rank = 3,
                podiumHeight = 70.dp,
                badgeColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumColumn(
    player: RoomPlayer,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val avatar = PRESET_AVATARS.find { it.id == player.avatarId } ?: PRESET_AVATARS.first()

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar + Crown
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 56.dp else 44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(avatar.jerseyColorHex), Color(avatar.accentColorHex))
                        )
                    )
                    .border(2.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = avatar.emoji, fontSize = if (rank == 1) 26.sp else 20.sp)
            }

            // Rank Badge Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (rank) {
                        1 -> "🥇 1st"
                        2 -> "🥈 2nd"
                        3 -> "🥉 3rd"
                        else -> "$rank"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = player.playerName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = "${player.score} Poin",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = badgeColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pedestal block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            badgeColor.copy(alpha = 0.4f),
                            Color(0xFF1E293B).copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "#${player.jerseyNumber}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun PlayerLeaderboardList(
    players: List<RoomPlayer>,
    currentUserId: String? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(players) { index, player ->
            val isMe = player.id == currentUserId
            val avatar = PRESET_AVATARS.find { it.id == player.avatarId } ?: PRESET_AVATARS.first()
            val accuracy = if (player.totalShots > 0) ((player.totalGoals.toFloat() / player.totalShots) * 100).toInt() else 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leaderboard_row_${player.id}")
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isMe) Color(0xFF0369A1).copy(alpha = 0.4f)
                        else Color(0xFF1E293B).copy(alpha = 0.7f)
                    )
                    .border(
                        1.dp,
                        if (isMe) Color(0xFF38BDF8) else Color(0xFF334155),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Number / Icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when (index) {
                                0 -> Color(0xFFF59E0B)
                                1 -> Color(0xFF94A3B8)
                                2 -> Color(0xFFD97706)
                                else -> Color(0xFF334155)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (index < 3) Color(0xFF0F172A) else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(avatar.jerseyColorHex), Color(avatar.accentColorHex))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.emoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Stats
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.playerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF38BDF8))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ANDA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                    Text(
                        text = "2PT: ${player.twoPointsScored} | 3PT: ${player.threePointsScored} | Akurasi: $accuracy%",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Total Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${player.score}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (index == 0) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                    )
                    Text(
                        text = "POIN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PRESET_AVATARS
import com.example.ui.viewmodel.BasketballViewModel

@Composable
fun GroupLobbyScreen(
    viewModel: BasketballViewModel,
    onMatchStarted: () -> Unit,
    onBackToHome: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsState()
    val activeRoom by viewModel.activeTournamentRoom.collectAsState()
    val players by viewModel.tournamentPlayers.collectAsState()
    val countdown by viewModel.tournamentCountdown.collectAsState()

    var roomCodeInput by remember { mutableStateOf(activeRoom?.roomCode ?: "BASKET-99") }
    var isInWaitingRoom by remember { mutableStateOf(activeRoom != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // When countdown completes and match starts, navigate to match
    LaunchedEffect(activeRoom?.isStarted, countdown) {
        if (activeRoom?.isStarted == true && activeRoom?.isFinished == false && countdown == null) {
            onMatchStarted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF090D16))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isInWaitingRoom) "Ruang Tunggu Turnamen" else "Gabung Turnamen Kelompok",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (isInWaitingRoom) "Kode Room: ${activeRoom?.roomCode}" else "Masukkan Kode Room dari Admin",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "By Cikgu Dasril Amri",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFEF08A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isInWaitingRoom) {
                // JOIN ROOM INPUT VIEW
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Gabung ke Room Pertandingan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Minta kode room 4-8 karakter kepada Admin yang membuat turnamen",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase() },
                            label = { Text("Kode Room") },
                            placeholder = { Text("Contoh: BASKET-99") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_code_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (roomCodeInput.isBlank()) {
                                    errorMessage = "Kode Room tidak boleh kosong"
                                    return@Button
                                }
                                val joined = viewModel.joinTournamentRoom(roomCodeInput)
                                if (joined) {
                                    isInWaitingRoom = true
                                    errorMessage = null
                                } else {
                                    // If room doesn't exist yet, auto create a friendly default room for demo convenience
                                    viewModel.createTournamentRoom(
                                        roomCode = roomCodeInput,
                                        roomName = "Turnamen Basket #$roomCodeInput",
                                        durationSeconds = 60,
                                        targetScore = 30,
                                        gameMode = "Turnamen Standar (2PT & 3PT)",
                                        hoopSpeed = "Sedang"
                                    )
                                    viewModel.joinTournamentRoom(roomCodeInput)
                                    isInWaitingRoom = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_room_btn")
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Masuk Ruang Tunggu",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // WAITING ROOM VIEW (RUANG TUNGGU)
                val room = activeRoom
                val infinitePulse = rememberInfiniteTransition(label = "waitingPulse")
                val pulseAlpha by infinitePulse.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                // Status Banner: Waiting for Admin
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = pulseAlpha))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Menunggu Admin Memulai Pertandingan...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Durasi: ${room?.durationSeconds}s | Mode: ${room?.gameMode}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-Time Joined Players Roster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pemain Terhubung (${players.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-Time Sync ⚡",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players) { player ->
                        val isMe = player.id == profile.id
                        val avatar = PRESET_AVATARS.find { it.id == player.avatarId } ?: PRESET_AVATARS.first()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isMe) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.7f))
                                .border(
                                    1.dp,
                                    if (isMe) Color(0xFF38BDF8) else Color(0xFF334155),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(avatar.jerseyColorHex), Color(avatar.accentColorHex))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = avatar.emoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

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
                                        Text(
                                            text = "(Anda)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = "Jersey #${player.jerseyNumber}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SIAP 🏀",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Leave Waiting Room button
                OutlinedButton(
                    onClick = {
                        viewModel.removePlayerFromRoom(profile.id)
                        isInWaitingRoom = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B))
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Keluar Ruang Tunggu", color = Color(0xFFEF4444))
                }
            }
        }

        // Live Countdown Overlay (3, 2, 1, GO!)
        if (countdown != null) {
            val count = countdown ?: 0
            val infiniteScale = rememberInfiniteTransition(label = "countdownScale")
            val scale by infiniteScale.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (count > 0) "$count" else "MULAI! 🏀",
                        fontSize = if (count > 0) 90.sp else 54.sp,
                        fontWeight = FontWeight.Black,
                        color = if (count > 0) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        modifier = Modifier.scale(scale)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bersiap menembak!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

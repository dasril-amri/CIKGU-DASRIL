package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PRESET_AVATARS
import com.example.ui.components.LeaderboardPodium
import com.example.ui.components.PlayerLeaderboardList
import com.example.ui.viewmodel.BasketballViewModel

@Composable
fun AdminPortalScreen(
    viewModel: BasketballViewModel,
    onBackToHome: () -> Unit
) {
    val isAuthenticated by viewModel.isAdminAuthenticated.collectAsState()
    val activeRoom by viewModel.activeTournamentRoom.collectAsState()
    val players by viewModel.tournamentPlayers.collectAsState()
    val currentPin by viewModel.adminPassword.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    // Room Configuration Form State
    var roomCode by remember { mutableStateOf(activeRoom?.roomCode ?: "BASKET-77") }
    var roomName by remember { mutableStateOf(activeRoom?.roomName ?: "Turnamen Basket Pelajar") }
    var selectedDuration by remember { mutableIntStateOf(activeRoom?.durationSeconds ?: 60) }
    var selectedTargetScore by remember { mutableIntStateOf(activeRoom?.targetScore ?: 30) }
    var selectedGameMode by remember { mutableStateOf(activeRoom?.gameMode ?: "Standar (2PT & 3PT)") }
    var selectedHoopSpeed by remember { mutableStateOf(activeRoom?.hoopSpeed ?: "Sedang") }

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
        if (!isAuthenticated) {
            // ADMIN LOGIN PASSWORD SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                        .border(2.dp, Color(0xFFF59E0B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Lock",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Akses Kontrol Admin",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Masukkan PIN / Password Admin untuk mengelola turnamen kelompok (Default: admin123)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Created By : Cikgu Dasril Amri",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFEF08A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = false
                            },
                            label = { Text("Password Admin") },
                            placeholder = { Text("Masukkan password...") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = passwordError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input")
                        )

                        if (passwordError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Password salah! Silakan coba lagi.",
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val success = viewModel.verifyAdminPassword(passwordInput)
                                if (!success) {
                                    passwordError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_login_btn")
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Masuk Sebagai Admin",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBackToHome) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Kembali ke Menu", color = Color(0xFF94A3B8))
                }
            }
        } else {
            // ADMIN DASHBOARD & CONTROLS
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Header with Back, Title & Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                text = "Panel Kontrol Admin",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Created By : Cikgu Dasril Amri",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "Ubah Password", tint = Color(0xFF38BDF8))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 1: ROOM SETUP / CONFIGURATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pengaturan Room Pertandingan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Room Code & Name
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = roomCode,
                                onValueChange = { roomCode = it.uppercase() },
                                label = { Text("Kode Room") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                label = { Text("Nama Turnamen") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Duration Selector
                        Text(text = "Durasi Pertandingan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(30 to "30s", 60 to "60s", 90 to "90s", 120 to "120s").forEach { (dur, label) ->
                                val isSelected = selectedDuration == dur
                                Surface(
                                    onClick = { selectedDuration = dur },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Score
                        Text(text = "Skor Target Selesai:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(21 to "21 Poin", 30 to "30 Poin", 50 to "50 Poin", 0 to "Waktu Saja").forEach { (sc, label) ->
                                val isSelected = selectedTargetScore == sc
                                Surface(
                                    onClick = { selectedTargetScore = sc },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Save / Update Room Button
                        Button(
                            onClick = {
                                viewModel.createTournamentRoom(
                                    roomCode = roomCode,
                                    roomName = roomName,
                                    durationSeconds = selectedDuration,
                                    targetScore = selectedTargetScore,
                                    gameMode = selectedGameMode,
                                    hoopSpeed = selectedHoopSpeed
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terapkan Pengaturan Room", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 2: MATCH CONTROLS (Mulai, Jeda, Berhenti)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kontrol Laga Turnamen",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Status Pill
                            val isRunning = activeRoom?.isStarted == true && activeRoom?.isFinished == false
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRunning) Color(0xFF10B981) else Color(0xFF64748B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (activeRoom?.isFinished == true) "SELESAI 🏁" else if (isRunning) "BERJALAN (${activeRoom?.remainingSeconds}s) ⏱️" else "RUANG TUNGGU ⏳",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mulai Permainan
                            Button(
                                onClick = { viewModel.startTournamentMatch() },
                                enabled = activeRoom != null && (activeRoom?.isStarted == false || activeRoom?.isFinished == true),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_start_match_btn")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mulai", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Jeda
                            Button(
                                onClick = { viewModel.pauseTournamentMatch() },
                                enabled = activeRoom?.isStarted == true && activeRoom?.isFinished == false,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeRoom?.isPaused == true) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeRoom?.isPaused == true) "Lanjut" else "Jeda",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp
                                )
                            }

                            // Hentikan
                            Button(
                                onClick = { viewModel.finishTournamentMatch() },
                                enabled = activeRoom?.isStarted == true && activeRoom?.isFinished == false,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Demo Competitor & Reset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.addDemoCompetitor(roomCode) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Tambah Lawan", fontSize = 12.sp, color = Color(0xFF38BDF8))
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetTournament() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 3: REALTIME LIVE LEADERBOARD / WAITING ROSTER
                Text(
                    text = "Papan Skor & Daftar Pemain Terhubung (${players.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (players.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.PeopleOutline, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Belum ada pemain di room ini", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(text = "Klik '+ Tambah Lawan' atau minta pemain gabung dengan kode $roomCode", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                } else {
                    // Show Podium if finished or has top players
                    if (activeRoom?.isFinished == true) {
                        LeaderboardPodium(players = players)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Player List
                    PlayerLeaderboardList(players = players, modifier = Modifier.heightIn(max = 350.dp))
                }
            }
        }

        // Change Admin Password Dialog
        if (showChangePinDialog) {
            var newPinInput by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showChangePinDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Ubah Password Admin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { newPinInput = it },
                            label = { Text("Password Baru") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (newPinInput.isNotBlank()) {
                                    viewModel.updateAdminPassword(newPinInput)
                                }
                                showChangePinDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text("Simpan Password", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.AvatarPicker
import com.example.ui.viewmodel.BasketballViewModel

@Composable
fun HomeScreen(
    viewModel: BasketballViewModel,
    onStartSoloMode: (GameMode, HoopSpeed) -> Unit,
    onNavigateToGroupLobby: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showSoloModeSelector by remember { mutableStateOf(false) }
    var selectedSoloMode by remember { mutableStateOf(GameMode.SOLO_FREE) }
    var selectedHoopSpeed by remember { mutableStateOf(HoopSpeed.MEDIUM) }

    val currentAvatar = PRESET_AVATARS.find { it.id == profile.avatarId } ?: PRESET_AVATARS.first()
    val currentBallSkin = PRESET_BALL_SKINS.find { it.id == profile.ballSkinId } ?: PRESET_BALL_SKINS.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Sound toggle & Admin Access Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .testTag("mute_toggle_button")
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Sound Toggle",
                    tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF38BDF8)
                )
            }

            // Admin Portal Access Button
            Button(
                onClick = onNavigateToAdmin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("admin_portal_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Akses Admin",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Title & Logo Badge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEA580C), Color(0xFFF59E0B))
                        )
                    )
                    .border(2.dp, Color(0xFFFED7AA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsBasketball,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SLAM DUNK",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Color.White
            )
            Text(
                text = "BASKETBALL ARENA",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Creator Badge (Created By : Cikgu Dasril Amri)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Created By : Cikgu Dasril Amri",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFEF08A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Player Profile Card (Avatar, Name, Jersey, Stats)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_profile_card")
                .clickable { showProfileDialog = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(currentAvatar.jerseyColorHex), Color(currentAvatar.accentColorHex))
                            )
                        )
                        .border(2.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = currentAvatar.emoji, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF38BDF8))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${profile.jerseyNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    Text(
                        text = "Skin: ${currentBallSkin.name} | Rekor: ${profile.highestScoreSolo} Poin",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Menu Game Buttons
        Text(
            text = "PILIH MODE PERMAINAN",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Bermain Sendiri (Solo Mode) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("solo_game_mode_card")
                .clickable { showSoloModeSelector = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
            border = BorderStroke(1.5.dp, Color(0xFFEA580C).copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFEA580C), Color(0xFFDC2626))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bermain Sendiri",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Bebas tanpa aturan admin, langsung main! Latihan 2PT & 3PT, Ring Bergerak, Arcade.",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Bermain Kelompok (Group Tournament) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("group_game_mode_card")
                .clickable { onNavigateToGroupLobby() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
            border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bermain Kelompok",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Gabung ke Ruang Tunggu dengan Kode Room. Diatur serentak oleh Admin & Live Skor!",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Riwayat & Statistik
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stats_history_card")
                .clickable { onNavigateToStats() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Papan Skor & Riwayat Laga",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Lihat rekor akurasi dan trofi pertandingan sebelumnya",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Creator Signature Card (Created By : Cikgu Dasril Amri)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Created By : Cikgu Dasril Amri",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFEF08A)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aplikasi Game Menembak Bola Basket Digital Interaktif",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // Modal: Solo Mode Picker Dialog
    if (showSoloModeSelector) {
        Dialog(onDismissRequest = { showSoloModeSelector = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PILIH MODE SOLO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Pilih tipe tantangan menembak bola basket",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GameMode.values().filter { it != GameMode.GROUP_TOURNAMENT }.forEach { mode ->
                        val isSelected = selectedSoloMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.4f))
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedSoloMode = mode }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedSoloMode = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                )
                                Text(
                                    text = mode.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // If Moving Hoop selected, show speed selector
                    if (selectedSoloMode == GameMode.SOLO_MOVING_HOOP) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Kecepatan Gerak Ring:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HoopSpeed.values().forEach { speed ->
                                val isSpeedSelected = selectedHoopSpeed == speed
                                Surface(
                                    onClick = { selectedHoopSpeed = speed },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSpeedSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = speed.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSpeedSelected) Color(0xFF0F172A) else Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showSoloModeSelector = false
                            onStartSoloMode(selectedSoloMode, selectedHoopSpeed)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_start_solo_btn")
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mulai Menembak!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Modal: Edit Profile & Avatar Dialog
    if (showProfileDialog) {
        var tempName by remember(profile.name) { mutableStateOf(profile.name) }
        var tempNumber by remember(profile.jerseyNumber) { mutableIntStateOf(profile.jerseyNumber) }
        var tempAvatarId by remember(profile.avatarId) { mutableStateOf(profile.avatarId) }
        var tempBallSkinId by remember(profile.ballSkinId) { mutableStateOf(profile.ballSkinId) }

        Dialog(onDismissRequest = { showProfileDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Profil & Personalisasi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Player Name Input
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nama Pemain") },
                        placeholder = { Text("Ketik nama pemain...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Jersey Number Input
                    OutlinedTextField(
                        value = "$tempNumber",
                        onValueChange = { tempNumber = it.toIntOrNull()?.coerceIn(0, 99) ?: 0 },
                        label = { Text("Nomor Jersey (0 - 99)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_number_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Avatar & Ball Skin Picker
                    AvatarPicker(
                        selectedAvatarId = tempAvatarId,
                        selectedBallSkinId = tempBallSkinId,
                        jerseyNumber = tempNumber,
                        onAvatarSelected = {
                            tempAvatarId = it.id
                            tempNumber = it.defaultNumber
                        },
                        onBallSkinSelected = { tempBallSkinId = it.id },
                        onJerseyNumberChanged = { tempNumber = it }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            viewModel.saveFullProfile(
                                name = tempName,
                                jerseyNumber = tempNumber,
                                avatarId = tempAvatarId,
                                ballSkinId = tempBallSkinId
                            )
                            showProfileDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_btn")
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan Profil",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

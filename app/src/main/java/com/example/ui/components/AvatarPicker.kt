package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
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

@Composable
fun AvatarPicker(
    selectedAvatarId: String,
    selectedBallSkinId: String,
    jerseyNumber: Int,
    onAvatarSelected: (Avatar) -> Unit,
    onBallSkinSelected: (BallSkin) -> Unit,
    onJerseyNumberChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.9f))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Section 1: Choose Character Avatar
        Text(
            text = "Pilih Avatar Pemain",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Karakter & Gaya Jersey Pemain",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(PRESET_AVATARS) { avatar ->
                val isSelected = avatar.id == selectedAvatarId
                val jerseyColor = Color(avatar.jerseyColorHex)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .testTag("avatar_item_${avatar.id}")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFF334155) else Color(0xFF0F172A).copy(alpha = 0.6f))
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onAvatarSelected(avatar) }
                        .padding(8.dp)
                        .width(76.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(jerseyColor, Color(avatar.accentColorHex))
                                )
                            )
                    ) {
                        Text(
                            text = avatar.emoji,
                            fontSize = 24.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(12.dp).align(Alignment.Center)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = avatar.name.split(" ").first(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "#${avatar.defaultNumber}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2: Choose Ball Skin
        Text(
            text = "Skin Bola Basket",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PRESET_BALL_SKINS) { skin ->
                val isSelected = skin.id == selectedBallSkinId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .testTag("skin_item_${skin.id}")
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF334155) else Color(0xFF0F172A).copy(alpha = 0.6f))
                        .border(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) Color(skin.glowColorHex) else Color(0xFF334155),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBallSkinSelected(skin) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(skin.primaryColorHex))
                            .border(1.dp, Color(skin.lineHex), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = skin.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(skin.glowColorHex) else Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

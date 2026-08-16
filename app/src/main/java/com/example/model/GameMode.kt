package com.example.model

enum class GameMode(val displayName: String, val description: String, val iconName: String) {
    SOLO_FREE("Latihan Bebas", "Tembak tanpa batas waktu, asah akurasi 2PT & 3PT", "SportsBasketball"),
    SOLO_TIME_ATTACK("Time Attack (60s)", "Cetak poin sebanyak mungkin dalam 60 detik dengan combo!", "Timer"),
    SOLO_MOVING_HOOP("Ring Bergerak", "Tantangan menembak ke ring yang bergerak dinamis", "SwapHoriz"),
    SOLO_3PT_CONTEST("Kontes 3-Poin", "Tembak dari 5 titik lingkaran 3-poin dengan Money Ball", "Stars"),
    SOLO_DISTANCE_LADDER("Tantangan Jarak", "Semakin banyak masuk, jarak tembak semakin jauh", "Straighten"),
    GROUP_TOURNAMENT("Turnamen Kelompok", "Laga bersama diatur penuh oleh Admin dengan Live Skor", "Groups")
}

enum class ShotZone(val points: Int, val displayName: String, val distancePxRatio: Float) {
    TWO_POINT_CLOSE(2, "2 Poin (Dekat)", 0.35f),
    TWO_POINT_MID(2, "2 Poin (Mid-Range)", 0.52f),
    THREE_POINT(3, "3 Poin (Standar)", 0.72f),
    DEEP_THREE(3, "3 Poin (Deep / Logo)", 0.88f)
}

enum class HoopSpeed(val displayName: String, val speedFactor: Float) {
    SLOW("Lambat", 1.0f),
    MEDIUM("Sedang", 1.8f),
    FAST("Cepat", 2.6f),
    EXTREME("Ekstrem", 3.5f)
}

data class Avatar(
    val id: String,
    val name: String,
    val title: String,
    val jerseyColorHex: Long,
    val accentColorHex: Long,
    val emoji: String,
    val defaultNumber: Int
)

val PRESET_AVATARS = listOf(
    Avatar("av_1", "Alex \"The Sniper\"", "Spesialis 3-Poin", 0xFFE11D48, 0xFFFBBF24, "🔥", 23),
    Avatar("av_2", "Budi \"Skywalker\"", "Dunk Master", 0xFF2563EB, 0xFF60A5FA, "⚡", 7),
    Avatar("av_3", "Rian \"Clutch\"", "Mid-Range King", 0xFF059669, 0xFF34D399, "🎯", 30),
    Avatar("av_4", "Doni \"The Beast\"", "Post Scorer", 0xFF7C3AED, 0xFFA78BFA, "👑", 34),
    Avatar("av_5", "Maya \"Flash\"", "Speed Shooter", 0xFFEA580C, 0xFFFDBA74, "💫", 11),
    Avatar("av_6", "Kenzo \"Samurai\"", "Curry Style", 0xFF0284C7, 0xFF38BDF8, "🏀", 3),
    Avatar("av_7", "Rafi \"Mamba\"", "Fadeaway Ace", 0xFFD97706, 0xFFFCD34D, "🐍", 24),
    Avatar("av_8", "Siti \"Iceman\"", "Cold Blooded", 0xFF0D9488, 0xFF5EEAD4, "❄️", 8)
)

data class BallSkin(
    val id: String,
    val name: String,
    val primaryColorHex: Long,
    val lineHex: Long,
    val glowColorHex: Long,
    val trailColorHex: Long
)

val PRESET_BALL_SKINS = listOf(
    BallSkin("skin_classic", "Klasik NBA", 0xFFEA580C, 0xFF1E293B, 0xFFFDBA74, 0x66EA580C),
    BallSkin("skin_fire", "Inferno Flame", 0xFFDC2626, 0xFFFEF08A, 0xFFF87171, 0x88EF4444),
    BallSkin("skin_cyber", "Cyber Neon", 0xFF06B6D4, 0xFF0F172A, 0xFF67E8F9, 0x8806B6D4),
    BallSkin("skin_gold", "Championship Gold", 0xFFEAB308, 0xFF78350F, 0xFFFEF08A, 0x88EAB308),
    BallSkin("skin_purple", "Midnight Stealth", 0xFF8B5CF6, 0xFF1E1B4B, 0xFFC4B5FD, 0x888B5CF6)
)

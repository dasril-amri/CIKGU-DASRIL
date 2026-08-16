package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profiles")
data class PlayerProfile(
    @PrimaryKey val id: String = "default_player",
    val name: String = "Shooter Ace",
    val avatarId: String = "av_1",
    val jerseyNumber: Int = 23,
    val ballSkinId: String = "skin_classic",
    val totalScore: Int = 0,
    val totalShots: Int = 0,
    val totalGoals: Int = 0,
    val highestScoreSolo: Int = 0,
    val highestStreak: Int = 0,
    val matchesPlayed: Int = 0
)

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameMode: String,
    val score: Int,
    val totalShots: Int,
    val totalGoals: Int,
    val twoPointsScored: Int,
    val threePointsScored: Int,
    val swishCount: Int,
    val highestStreak: Int,
    val accuracyPercent: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tournament_rooms")
data class TournamentRoom(
    @PrimaryKey val roomCode: String,
    val roomName: String,
    val adminPin: String = "admin123",
    val durationSeconds: Int = 60,
    val targetScore: Int = 30,
    val gameMode: String = "Turnamen Standar",
    val hoopSpeed: String = "Sedang",
    val isStarted: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val remainingSeconds: Int = 60,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_players")
data class RoomPlayer(
    @PrimaryKey val id: String,
    val roomCode: String,
    val playerName: String,
    val avatarId: String,
    val jerseyNumber: Int,
    val score: Int = 0,
    val twoPointsScored: Int = 0,
    val threePointsScored: Int = 0,
    val totalShots: Int = 0,
    val totalGoals: Int = 0,
    val highestStreak: Int = 0,
    val isReady: Boolean = true,
    val rank: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

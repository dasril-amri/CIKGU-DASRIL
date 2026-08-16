package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MatchRecord
import com.example.data.local.entity.PlayerProfile
import com.example.data.local.entity.RoomPlayer
import com.example.data.local.entity.TournamentRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface BasketballDao {
    // Player Profile
    @Query("SELECT * FROM player_profiles WHERE id = :id LIMIT 1")
    fun getPlayerProfileFlow(id: String = "default_player"): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profiles WHERE id = :id LIMIT 1")
    suspend fun getPlayerProfile(id: String = "default_player"): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfile)

    // Match Records
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatchRecordsFlow(): Flow<List<MatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchRecord(record: MatchRecord): Long

    @Query("SELECT MAX(score) FROM match_records WHERE gameMode = :gameMode")
    suspend fun getHighScoreByMode(gameMode: String): Int?

    // Tournament Room
    @Query("SELECT * FROM tournament_rooms WHERE roomCode = :roomCode LIMIT 1")
    fun getTournamentRoomFlow(roomCode: String): Flow<TournamentRoom?>

    @Query("SELECT * FROM tournament_rooms WHERE roomCode = :roomCode LIMIT 1")
    suspend fun getTournamentRoom(roomCode: String): TournamentRoom?

    @Query("SELECT * FROM tournament_rooms ORDER BY createdAt DESC")
    fun getAllRoomsFlow(): Flow<List<TournamentRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRoom(room: TournamentRoom)

    @Query("DELETE FROM tournament_rooms WHERE roomCode = :roomCode")
    suspend fun deleteRoom(roomCode: String)

    // Room Players
    @Query("SELECT * FROM room_players WHERE roomCode = :roomCode ORDER BY score DESC, updatedAt ASC")
    fun getRoomPlayersFlow(roomCode: String): Flow<List<RoomPlayer>>

    @Query("SELECT * FROM room_players WHERE roomCode = :roomCode ORDER BY score DESC, updatedAt ASC")
    suspend fun getRoomPlayers(roomCode: String): List<RoomPlayer>

    @Query("SELECT * FROM room_players WHERE roomCode = :roomCode AND id = :playerId LIMIT 1")
    suspend fun getRoomPlayer(roomCode: String, playerId: String): RoomPlayer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRoomPlayer(player: RoomPlayer)

    @Query("DELETE FROM room_players WHERE roomCode = :roomCode AND id = :playerId")
    suspend fun removePlayerFromRoom(roomCode: String, playerId: String)

    @Query("DELETE FROM room_players WHERE roomCode = :roomCode")
    suspend fun clearRoomPlayers(roomCode: String)
}

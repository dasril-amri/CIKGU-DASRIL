package com.example.data.local

import com.example.data.local.dao.BasketballDao
import com.example.data.local.entity.MatchRecord
import com.example.data.local.entity.PlayerProfile
import com.example.data.local.entity.RoomPlayer
import com.example.data.local.entity.TournamentRoom
import kotlinx.coroutines.flow.Flow

class BasketballRepository(private val dao: BasketballDao) {

    // Profile
    val playerProfile: Flow<PlayerProfile?> = dao.getPlayerProfileFlow()

    suspend fun getProfile(): PlayerProfile {
        return dao.getPlayerProfile() ?: PlayerProfile().also {
            dao.insertOrUpdateProfile(it)
        }
    }

    suspend fun saveProfile(profile: PlayerProfile) {
        dao.insertOrUpdateProfile(profile)
    }

    // Match Records
    val matchRecords: Flow<List<MatchRecord>> = dao.getAllMatchRecordsFlow()

    suspend fun saveMatchRecord(record: MatchRecord): Long {
        return dao.insertMatchRecord(record)
    }

    suspend fun getHighScore(gameMode: String): Int {
        return dao.getHighScoreByMode(gameMode) ?: 0
    }

    // Tournament Room
    fun getRoomFlow(roomCode: String): Flow<TournamentRoom?> = dao.getTournamentRoomFlow(roomCode)

    fun getAllRoomsFlow(): Flow<List<TournamentRoom>> = dao.getAllRoomsFlow()

    suspend fun getRoom(roomCode: String): TournamentRoom? = dao.getTournamentRoom(roomCode)

    suspend fun saveRoom(room: TournamentRoom) = dao.insertOrUpdateRoom(room)

    suspend fun deleteRoom(roomCode: String) {
        dao.deleteRoom(roomCode)
        dao.clearRoomPlayers(roomCode)
    }

    // Room Players
    fun getRoomPlayersFlow(roomCode: String): Flow<List<RoomPlayer>> = dao.getRoomPlayersFlow(roomCode)

    suspend fun getRoomPlayers(roomCode: String): List<RoomPlayer> = dao.getRoomPlayers(roomCode)

    suspend fun saveRoomPlayer(player: RoomPlayer) = dao.insertOrUpdateRoomPlayer(player)

    suspend fun removePlayer(roomCode: String, playerId: String) = dao.removePlayerFromRoom(roomCode, playerId)
}

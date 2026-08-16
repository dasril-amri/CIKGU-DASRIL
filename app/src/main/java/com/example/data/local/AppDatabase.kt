package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BasketballDao
import com.example.data.local.entity.MatchRecord
import com.example.data.local.entity.PlayerProfile
import com.example.data.local.entity.RoomPlayer
import com.example.data.local.entity.TournamentRoom

@Database(
    entities = [
        PlayerProfile::class,
        MatchRecord::class,
        TournamentRoom::class,
        RoomPlayer::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun basketballDao(): BasketballDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "basketball_game_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.GameProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY isFavorite DESC, lastUsedTimestamp DESC, appName ASC")
    fun getAllProfilesFlow(): Flow<List<GameProfile>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :pkg LIMIT 1")
    suspend fun getProfile(pkg: String): GameProfile?

    @Query("SELECT * FROM game_profiles WHERE isFavorite = 1 ORDER BY lastUsedTimestamp DESC")
    fun getFavoritesFlow(): Flow<List<GameProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: GameProfile)

    @Update
    suspend fun update(profile: GameProfile)

    @Delete
    suspend fun delete(profile: GameProfile)

    @Query("DELETE FROM game_profiles WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("UPDATE game_profiles SET lastUsedTimestamp = :timestamp WHERE packageName = :pkg")
    suspend fun updateLastUsed(pkg: String, timestamp: Long)
}

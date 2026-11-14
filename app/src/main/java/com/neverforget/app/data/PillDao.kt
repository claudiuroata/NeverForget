package com.neverforget.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PillDao {

    @Query("SELECT * FROM pills ORDER BY createdAt DESC")
    fun getAllPills(): Flow<List<Pill>>

    @Query("SELECT * FROM pills ORDER BY createdAt DESC")
    suspend fun getAllPillsSync(): List<Pill>

    @Query("SELECT * FROM pills WHERE id = :id LIMIT 1")
    suspend fun getPillById(id: Long): Pill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPill(pill: Pill): Long

    @Update
    suspend fun updatePill(pill: Pill)

    @Delete
    suspend fun deletePill(pill: Pill)
}

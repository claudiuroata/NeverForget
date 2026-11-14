package com.neverforget.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PillRepository(private val pillDao: PillDao) {

    val allPills: Flow<List<Pill>> = pillDao.getAllPills()

    suspend fun getAllPillsSync(): List<Pill> {
        return pillDao.getAllPillsSync()
    }

    suspend fun getPillById(id: Long): Pill? {
        return pillDao.getPillById(id)
    }

    suspend fun insertPill(pill: Pill): Long {
        return pillDao.insertPill(pill)
    }

    suspend fun updatePill(pill: Pill) {
        pillDao.updatePill(pill)
    }

    suspend fun deletePill(pill: Pill) {
        pillDao.deletePill(pill)
    }

    suspend fun toggleDoseTaken(pill: Pill, date: LocalDate, timeSlot: String) {
        val updated = pill.toggleDoseTaken(date, timeSlot)
        pillDao.updatePill(updated)
    }

    suspend fun markDoseTaken(pill: Pill, date: LocalDate, timeSlot: String) {
        val updated = pill.markDoseTaken(date, timeSlot)
        pillDao.updatePill(updated)
    }
}

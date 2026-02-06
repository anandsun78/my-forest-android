package com.anandsundaram.myforest.data

import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    fun observeSessions(): Flow<List<FocusSessionEntity>>
    suspend fun insertSession(session: FocusSessionEntity)
}

class RoomFocusRepository(
    private val dao: FocusSessionDao
) : FocusRepository {
    override fun observeSessions(): Flow<List<FocusSessionEntity>> = dao.observeSessions()

    override suspend fun insertSession(session: FocusSessionEntity) {
        dao.insertSession(session)
    }
}

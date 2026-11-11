package com.winsongr.saga.orchestrator.persistence

import com.winsongr.saga.orchestrator.domain.Saga
import com.winsongr.saga.orchestrator.domain.SagaStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@Repository
class SagaRepository(private val jdbc: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        Saga(
            id = UUID.fromString(rs.getString("id")),
            type = rs.getString("type"),
            status = SagaStatus.valueOf(rs.getString("status")),
            payload = rs.getString("payload"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
            completedAt = rs.getTimestamp("completed_at")?.toInstant()
        )
    }

    fun insert(saga: Saga) {
        jdbc.update(
            """INSERT INTO sagas (id, type, status, payload, created_at, updated_at, completed_at)
               VALUES (?::uuid, ?, ?, ?::jsonb, ?, ?, ?)""",
            saga.id.toString(),
            saga.type,
            saga.status.name,
            saga.payload,
            java.sql.Timestamp.from(saga.createdAt),
            java.sql.Timestamp.from(saga.updatedAt),
            saga.completedAt?.let { java.sql.Timestamp.from(it) }
        )
    }

    fun findById(id: UUID): Saga? {
        return jdbc.query(
            "SELECT * FROM sagas WHERE id = ?::uuid",
            rowMapper,
            id.toString()
        ).firstOrNull()
    }

    /**
     * SELECT ... FOR UPDATE — prevents concurrent mutations on the same saga.
     * Must be called within a @Transactional method.
     */
    fun findForUpdate(id: UUID): Saga? {
        return jdbc.query(
            "SELECT * FROM sagas WHERE id = ?::uuid FOR UPDATE",
            rowMapper,
            id.toString()
        ).firstOrNull()
    }

    fun updateStatus(id: UUID, status: SagaStatus, completedAt: Instant? = null) {
        jdbc.update(
            """UPDATE sagas SET status = ?, updated_at = NOW(), completed_at = ?
               WHERE id = ?::uuid""",
            status.name,
            completedAt?.let { java.sql.Timestamp.from(it) },
            id.toString()
        )
    }

    fun findByStatus(status: SagaStatus, limit: Int = 50): List<Saga> {
        return jdbc.query(
            "SELECT * FROM sagas WHERE status = ? ORDER BY updated_at DESC LIMIT ?",
            rowMapper,
            status.name,
            limit
        )
    }
}

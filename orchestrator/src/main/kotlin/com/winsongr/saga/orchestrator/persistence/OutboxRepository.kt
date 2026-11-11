package com.winsongr.saga.orchestrator.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class OutboxRow(
    val id: Long = 0,
    val sagaId: UUID,
    val topic: String,
    val key: String,
    val payload: String,      // JSON
    val headers: String,      // JSON map
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val attempts: Int = 0
)

@Repository
class OutboxRepository(private val jdbc: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        OutboxRow(
            id = rs.getLong("id"),
            sagaId = UUID.fromString(rs.getString("saga_id")),
            topic = rs.getString("topic"),
            key = rs.getString("key"),
            payload = rs.getString("payload"),
            headers = rs.getString("headers"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            sentAt = rs.getTimestamp("sent_at")?.toInstant(),
            attempts = rs.getInt("attempts")
        )
    }

    fun insert(row: OutboxRow) {
        jdbc.update(
            """INSERT INTO outbox (saga_id, topic, key, payload, headers, created_at, sent_at, attempts)
               VALUES (?::uuid, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)""",
            row.sagaId.toString(),
            row.topic,
            row.key,
            row.payload,
            row.headers,
            java.sql.Timestamp.from(row.createdAt),
            row.sentAt?.let { java.sql.Timestamp.from(it) },
            row.attempts
        )
    }

    /**
     * Fetch unsent outbox rows in FIFO order.
     * Uses partial index idx_outbox_unsent_created (WHERE sent_at IS NULL).
     */
    fun fetchUnsent(limit: Int = 100): List<OutboxRow> {
        return jdbc.query(
            "SELECT * FROM outbox WHERE sent_at IS NULL ORDER BY created_at LIMIT ?",
            rowMapper,
            limit
        )
    }

    fun markSent(id: Long) {
        jdbc.update(
            "UPDATE outbox SET sent_at = NOW() WHERE id = ?",
            id
        )
    }

    fun incrementAttempts(id: Long) {
        jdbc.update(
            "UPDATE outbox SET attempts = attempts + 1 WHERE id = ?",
            id
        )
    }
}

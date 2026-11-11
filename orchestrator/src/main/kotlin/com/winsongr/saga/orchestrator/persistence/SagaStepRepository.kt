package com.winsongr.saga.orchestrator.persistence

import com.winsongr.saga.orchestrator.domain.SagaStep
import com.winsongr.saga.orchestrator.domain.StepStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class SagaStepRepository(private val jdbc: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        SagaStep(
            sagaId = UUID.fromString(rs.getString("saga_id")),
            stepName = rs.getString("step_name"),
            sequenceNo = rs.getInt("sequence_no"),
            status = StepStatus.valueOf(rs.getString("status")),
            requestPayload = rs.getString("request_payload"),
            responsePayload = rs.getString("response_payload"),
            errorMessage = rs.getString("error_message"),
            startedAt = rs.getTimestamp("started_at")?.toInstant(),
            completedAt = rs.getTimestamp("completed_at")?.toInstant(),
            deadlineAt = rs.getTimestamp("deadline_at")?.toInstant(),
            attemptCount = rs.getInt("attempt_count")
        )
    }

    fun insert(step: SagaStep) {
        jdbc.update(
            """INSERT INTO saga_steps 
               (saga_id, step_name, sequence_no, status, request_payload, 
                response_payload, error_message, started_at, completed_at, deadline_at, attempt_count)
               VALUES (?::uuid, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?)""",
            step.sagaId.toString(),
            step.stepName,
            step.sequenceNo,
            step.status.name,
            step.requestPayload,
            step.responsePayload,
            step.errorMessage,
            step.startedAt?.let { java.sql.Timestamp.from(it) },
            step.completedAt?.let { java.sql.Timestamp.from(it) },
            step.deadlineAt?.let { java.sql.Timestamp.from(it) },
            step.attemptCount
        )
    }

    fun findBySagaId(sagaId: UUID): List<SagaStep> {
        return jdbc.query(
            "SELECT * FROM saga_steps WHERE saga_id = ?::uuid ORDER BY sequence_no",
            rowMapper,
            sagaId.toString()
        )
    }

    fun findByKey(sagaId: UUID, stepName: String): SagaStep? {
        return jdbc.query(
            "SELECT * FROM saga_steps WHERE saga_id = ?::uuid AND step_name = ?",
            rowMapper,
            sagaId.toString(),
            stepName
        ).firstOrNull()
    }

    fun updateStatus(
        sagaId: UUID,
        stepName: String,
        status: StepStatus,
        responsePayload: String? = null,
        errorMessage: String? = null
    ) {
        jdbc.update(
            """UPDATE saga_steps 
               SET status = ?, response_payload = ?::jsonb, error_message = ?, 
                   completed_at = CASE WHEN ? IN ('COMPLETED','FAILED','TIMED_OUT','COMPENSATED','COMPENSATION_FAILED') 
                                       THEN NOW() ELSE completed_at END
               WHERE saga_id = ?::uuid AND step_name = ?""",
            status.name,
            responsePayload,
            errorMessage,
            status.name,
            sagaId.toString(),
            stepName
        )
    }

    /**
     * Hot path for TimeoutSweeper: find steps past their deadline.
     * Uses partial index idx_saga_steps_running_deadline.
     */
    fun fetchTimedOut(limit: Int = 100): List<SagaStep> {
        return jdbc.query(
            """SELECT * FROM saga_steps 
               WHERE status IN ('RUNNING', 'COMPENSATING') 
                 AND deadline_at < NOW()
               ORDER BY deadline_at
               LIMIT ?""",
            rowMapper,
            limit
        )
    }

    /**
     * Find completed forward steps for a saga — used by CompensationHandler
     * to walk in reverse and enqueue compensation commands.
     */
    fun findCompletedSteps(sagaId: UUID): List<SagaStep> {
        return jdbc.query(
            """SELECT * FROM saga_steps 
               WHERE saga_id = ?::uuid AND status = 'COMPLETED'
               ORDER BY sequence_no DESC""",
            rowMapper,
            sagaId.toString()
        )
    }

    fun updateStatusToCompensating(sagaId: UUID, stepName: String) {
        jdbc.update(
            """UPDATE saga_steps 
               SET status = 'COMPENSATING', started_at = NOW(), 
                   deadline_at = NOW() + INTERVAL '30 seconds'
               WHERE saga_id = ?::uuid AND step_name = ?""",
            sagaId.toString(),
            stepName
        )
    }
}

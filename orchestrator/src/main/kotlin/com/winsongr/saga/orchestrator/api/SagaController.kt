package com.winsongr.saga.orchestrator.api

import com.winsongr.saga.orchestrator.api.dto.SagaResponse
import com.winsongr.saga.orchestrator.api.dto.StepResponse
import com.winsongr.saga.orchestrator.domain.SagaStatus
import com.winsongr.saga.orchestrator.persistence.SagaRepository
import com.winsongr.saga.orchestrator.persistence.SagaStepRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/v1/sagas")
class SagaController(
    private val sagaRepository: SagaRepository,
    private val stepRepository: SagaStepRepository
) {

    /**
     * GET /v1/sagas/{id}
     * Returns full saga state including all steps.
     */
    @GetMapping("/{id}")
    fun getSaga(@PathVariable id: String): ResponseEntity<SagaResponse> {
        val sagaId = UUID.fromString(id)
        val saga = sagaRepository.findById(sagaId)
            ?: return ResponseEntity.notFound().build()

        val steps = stepRepository.findBySagaId(sagaId)

        return ResponseEntity.ok(
            SagaResponse(
                sagaId = saga.id.toString(),
                type = saga.type,
                status = saga.status.name,
                steps = steps.map { step ->
                    StepResponse(
                        name = step.stepName,
                        status = step.status.name,
                        startedAt = step.startedAt,
                        completedAt = step.completedAt,
                        deadlineAt = step.deadlineAt,
                        errorMessage = step.errorMessage
                    )
                },
                createdAt = saga.createdAt,
                completedAt = saga.completedAt
            )
        )
    }

    /**
     * GET /v1/sagas?status=STUCK&limit=50
     * Operator dashboard view. Indexed by idx_sagas_status_updated.
     */
    @GetMapping
    fun listSagas(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ResponseEntity<List<SagaResponse>> {
        val sagas = if (status != null) {
            sagaRepository.findByStatus(SagaStatus.valueOf(status), limit)
        } else {
            sagaRepository.findByStatus(SagaStatus.RUNNING, limit) +
                sagaRepository.findByStatus(SagaStatus.STUCK, limit)
        }

        return ResponseEntity.ok(
            sagas.map { saga ->
                SagaResponse(
                    sagaId = saga.id.toString(),
                    type = saga.type,
                    status = saga.status.name,
                    createdAt = saga.createdAt,
                    completedAt = saga.completedAt
                )
            }
        )
    }
}

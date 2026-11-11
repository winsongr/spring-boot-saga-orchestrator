package com.winsongr.saga.orchestrator.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Observability configuration.
 *
 * Injects saga_id into MDC from the X-Saga-Id header (if present)
 * so all log lines within a request carry saga correlation.
 */
@Configuration
class ObservabilityConfig {

    @Bean
    fun sagaIdMdcFilter(): FilterRegistrationBean<Filter> {
        val filter = Filter { request: ServletRequest, response: ServletResponse, chain: FilterChain ->
            val httpRequest = request as HttpServletRequest
            val sagaId = httpRequest.getHeader("X-Saga-Id")
            if (sagaId != null) {
                MDC.put("saga_id", sagaId)
            }
            try {
                chain.doFilter(request, response)
            } finally {
                MDC.remove("saga_id")
            }
        }

        return FilterRegistrationBean(filter).apply {
            addUrlPatterns("/v1/*")
            order = 1
        }
    }
}

package com.ticketing.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            builder.withDetail(cb.getName(), Map.of(
                    "state", cb.getState().name(),
                    "failureRate", cb.getMetrics().getFailureRate() + "%",
                    "bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls(),
                    "failedCalls", cb.getMetrics().getNumberOfFailedCalls()
            ));
        }
        return builder.build();
    }
}
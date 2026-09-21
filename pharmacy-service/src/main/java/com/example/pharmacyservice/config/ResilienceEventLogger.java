package com.example.pharmacyservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình lắng nghe các sự kiện (Events) của Resilience4j:
 * - CircuitBreaker: ghi log chuyển đổi trạng thái (CLOSED -> OPEN -> HALF_OPEN).
 * - Retry: ghi log số lần thử lại kết nối.
 * - RateLimiter: ghi log khi một request vượt quá tần suất cho phép.
 */
@Slf4j
@Configuration
public class ResilienceEventLogger {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher()
                        .onStateTransition(event -> log.warn("⚡ [CIRCUIT-BREAKER] '{}' chuyển đổi trạng thái từ {} sang {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                        .onError(event -> log.warn("⚠️ [CIRCUIT-BREAKER] '{}' ghi nhận lỗi: {}",
                                event.getCircuitBreakerName(),
                                event.getThrowable() != null ? event.getThrowable().getMessage() : "Unknown error"))
                        .onCallNotPermitted(event -> log.warn("🚫 [CIRCUIT-BREAKER] '{}' từ chối thực thi do mạch đang OPEN (Fail-fast)",
                                event.getCircuitBreakerName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
            }
        };
    }

    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher()
                        .onRetry(event -> log.warn("🔄 [RETRY] Đang tự động thử lại lần thứ {} cho '{}'. Lỗi vừa gặp: {}",
                                event.getNumberOfRetryAttempts(),
                                event.getName(),
                                event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "Unknown"))
                        .onSuccess(event -> log.info("✅ [RETRY] Thử lại thành công sau {} lần thử cho '{}'",
                                event.getNumberOfRetryAttempts(),
                                event.getName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
            }
        };
    }

    @Bean
    public RegistryEventConsumer<RateLimiter> rateLimiterEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<RateLimiter> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher()
                        .onFailure(event -> log.warn("🛑 [RATE-LIMITER] Request bị chặn bởi '{}' do vượt quá tần suất cho phép",
                                event.getRateLimiterName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<RateLimiter> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<RateLimiter> entryReplacedEvent) {
            }
        };
    }
}

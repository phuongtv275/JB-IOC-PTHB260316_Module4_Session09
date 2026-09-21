package com.example.pharmacyservice.exception;

import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Bộ xử lý ngoại lệ tập trung toàn ứng dụng pharmacy-service (Global Exception Handler).
 * Đáp ứng nghiêm ngặt quy tắc AGENTS.md:
 * - Có handler cho các exception
 * - Đính kèm correlation_id để developer dễ debug
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getCorrelationId() {
        String id = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        return id != null ? id : "N/A";
    }

    /**
     * Xử lý lỗi validation DTO (@Valid, @NotNull, @Min, @NotBlank, v.v.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        String correlationId = getCorrelationId();
        log.warn("[VALIDATION-ERROR] [cid:{}] Dữ liệu đầu vào không hợp lệ: {}", correlationId, fieldErrors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Dữ liệu gửi lên không hợp lệ, vui lòng kiểm tra lại")
                .errors(fieldErrors)
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi vi phạm ràng buộc Constraint
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        String correlationId = getCorrelationId();
        log.warn("[CONSTRAINT-VIOLATION] [cid:{}] {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Vi phạm ràng buộc dữ liệu: " + ex.getMessage(),
                null,
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý ngoại lệ khi Rate Limiter từ chối request
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimiterException(RequestNotPermitted ex) {
        String correlationId = getCorrelationId();
        log.warn("[RATE-LIMIT-EXCEEDED] [cid:{}] Request bị giới hạn tần suất: {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Hệ thống đang quá tải hoặc bạn thao tác quá nhanh, vui lòng thử lại sau ít giây",
                ex.getMessage(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Xử lý ngoại lệ khi Circuit Breaker đang ở trạng thái OPEN
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Object>> handleCircuitBreakerOpenException(CallNotPermittedException ex) {
        String correlationId = getCorrelationId();
        log.error("[CIRCUIT-BREAKER-OPEN] [cid:{}] Dịch vụ đối tác đang ngắt mạch: {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Dịch vụ liên kết tạm thời không khả dụng. Mạch bảo vệ đang ngắt để bảo vệ hệ thống",
                ex.getMessage(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Xử lý ngoại lệ khi hết thời gian chờ TimeLimiter
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResponse<Object>> handleTimeoutException(TimeoutException ex) {
        String correlationId = getCorrelationId();
        log.error("[TIMEOUT-EXCEPTION] [cid:{}] Hết thời gian chờ phản hồi: {}", correlationId, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Yêu cầu xử lý vượt quá thời gian cho phép (Timeout)",
                ex.getMessage(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    /**
     * Bắt tất cả các lỗi không mong muốn còn lại (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        String correlationId = getCorrelationId();
        log.error("[INTERNAL-ERROR] [cid:{}] Lỗi máy chủ nội bộ: ", correlationId, ex);

        ApiResponse<Object> response = ApiResponse.error(
                "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau hoặc liên hệ quản trị viên",
                ex.getMessage(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

package com.example.pharmacyservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Chuẩn hóa cấu trúc phản hồi API (API Response Envelope).
 *
 * @param <T> Kiểu dữ liệu của payload trả về.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Object errors;
    private String correlationId;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, Object errors, String correlationId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

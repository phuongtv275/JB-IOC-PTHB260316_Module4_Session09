package com.example.pharmacyservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter bắt và thiết lập Correlation ID cho từng request HTTP trong pharmacy-service.
 * Đảm bảo:
 * 1. Bắt hoặc sinh mới Correlation ID cho mọi request.
 * 2. Đưa vào SLF4J MDC để tự động xuất hiện trên mọi dòng log cho developer debug.
 * 3. Đính kèm vào response header 'X-Correlation-Id'.
 * 4. Xóa dữ liệu MDC trong khối finally để tránh rò rỉ bộ nhớ giữa các thread.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String correlationId = extractCorrelationId(request);

        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;

        log.info("[HTTP-IN] {} {} | Client IP: {}", method, fullPath, request.getRemoteAddr());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[HTTP-OUT] {} {} | Status: {} | Duration: {}ms", method, fullPath, response.getStatus(), duration);
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = request.getHeader(REQUEST_ID_HEADER);
        }
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}

package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.response.ApiResponse;
import com.example.pharmacyservice.filter.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller cung cấp thông tin chi nhánh lấy từ Spring Cloud Config Server.
 */
@Slf4j
@RefreshScope
@RestController
@RequestMapping("/api/v1/branch")
public class BranchController {

    @Value("${app.branch-name:N/A}")
    private String branchName;

    @Value("${app.hotline:N/A}")
    private String hotline;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getBranchInfo() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        log.info("[BRANCH-INFO] [cid:{}] Lấy thông tin chi nhánh: {} - {}", correlationId, branchName, hotline);

        Map<String, String> info = new HashMap<>();
        info.put("branchName", branchName);
        info.put("hotline", hotline);

        return ResponseEntity.ok(ApiResponse.success(info, "Lấy thông tin chi nhánh thành công", correlationId));
    }
}

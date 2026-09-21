package com.example.pharmacyservice.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runner thực thi khi ứng dụng pharmacy-service khởi động thành công.
 * Đáp ứng yêu cầu Bài tập 1: Lấy cấu hình từ Config Server qua @Value và in ra thông tin chi nhánh, hotline.
 */
@Slf4j
@Component
public class PharmacyStartupRunner implements CommandLineRunner {

    @Value("${app.branch-name:Chưa cấu hình}")
    private String branchName;

    @Value("${app.hotline:Chưa cấu hình}")
    private String hotline;

    @Value("${spring.datasource.url:Chưa cấu hình}")
    private String datasourceUrl;

    @Override
    public void run(String... args) {
        log.info("================================================================================");
        log.info("🚀 [PHARMACY-SERVICE] KHỞI ĐỘNG THÀNH CÔNG VỚI SPRING CLOUD CONFIG SERVER!");
        log.info("📍 Chi nhánh hiệu thuốc : {}", branchName);
        log.info("📞 Hotline hỗ trợ        : {}", hotline);
        log.info("🗄️ Database kết nối      : {}", datasourceUrl);
        log.info("================================================================================");
    }
}

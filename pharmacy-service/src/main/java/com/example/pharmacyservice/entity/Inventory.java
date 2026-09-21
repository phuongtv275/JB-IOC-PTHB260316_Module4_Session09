package com.example.pharmacyservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ thông tin tồn kho thuốc cục bộ tại chi nhánh (Local Inventory).
 * Được sử dụng làm nguồn dữ liệu dự phòng khi kho tổng mất kết nối (Fallback).
 */
@Entity
@Table(name = "local_inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String medicineCode;

    @Column(nullable = false, length = 200)
    private String medicineName;

    @Column(nullable = false)
    private Integer localStockQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(length = 100)
    private String shelfLocation;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

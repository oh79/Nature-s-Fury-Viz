package com.springboot.lab03.dto;

import lombok.Data;

@Data
public class SearchCriteria {
    // --- 페이징 ---
    private int page = 1; // 페이지 번호 (기본 1)
    private int size = 10; // 페이지당 항목 수 (기본 10)

    // --- 기간 (YYYY-MM-DD) ---
    private String minDate; // 시작일
    private String maxDate; // 종료일

    // --- 규모 (지진) ---
    private Double minMagnitude; // 최소 규모
    private Double maxMagnitude; // 최대 규모

    // --- 화산 필터 ---
    private Integer minElevation; // 최소 고도 (m)
    private Integer minVei; // 최소 VEI

    // --- 쓰나미 필터 ---
    private Double minMaximumWaterHeight; // 최소 최대 수위 (m)
    private Integer minNumberOfRunup; // 최소 Runups

    // --- 위치 범위 ---
    private Double minLatitude; // 최소 위도
    private Double maxLatitude; // 최대 위도
    private Double minLongitude; // 최소 경도
    private Double maxLongitude; // 최대 경도

    // --- 근접 검색 (미사용) ---
    private Double latitude; // 중심 위도
    private Double longitude; // 중심 경도
    private Double distance; // 거리 (km)
} 
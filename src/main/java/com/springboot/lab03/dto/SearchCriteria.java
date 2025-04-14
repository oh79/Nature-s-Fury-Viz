package com.springboot.lab03.dto;

import lombok.Data;

@Data
public class SearchCriteria {
    // --- 페이징 ---
    private int page = 1; // 요청할 페이지 번호 (기본값 1) - API에 따라 1부터 시작해야 할 수도 있음
    private int size = 10; // 페이지당 항목 수 (기본값 10)

    // --- 발생 기간 (YYYY-MM-DD 형식) ---
    private String minDate; // 시작 날짜
    private String maxDate; // 종료 날짜

    // --- 규모 범위 (주로 지진용) ---
    private Double minMagnitude; // 최소 규모
    private Double maxMagnitude; // 최대 규모

    // --- 화산 관련 필터 ---
    private Integer minElevation; // 최소 고도 (화산용)
    private Integer minVei; // 최소 VEI (화산용)

    // --- 쓰나미 관련 필터 ---
    private Double minMaximumWaterHeight; // 최소 최대 수위 (쓰나미용)
    private Integer minNumberOfRunup; // 최소 Runups (쓰나미용)

    // --- 위도/경도 범위 ---
    private Double minLatitude; // 최소 위도
    private Double maxLatitude; // 최대 위도
    private Double minLongitude; // 최소 경도
    private Double maxLongitude; // 최대 경도

    // --- 특정 위치 및 거리 기반 검색 (근접 검색) ---
    private Double latitude; // 중심 위도
    private Double longitude; // 중심 경도
    private Double distance; // 거리 (km)

    // 참고: API가 연도(minYear, maxYear)만 지원하는 경우,
    // 서비스 구현체에서 minDate, maxDate를 기반으로 연도를 추출해야 할 수 있어.
    // 마찬가지로 다른 필드들도 실제 API 파라미터 형식에 맞게 변환이 필요할 수 있음.
} 
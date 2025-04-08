package com.springboot.lab02.model;

import lombok.Data;

@Data
public class Earthquake {
    private String id; // 지진 ID
    private Double magnitude; // 지진 규모
    private String place; // 지진 발생 장소
    private Long time; // 지진 발생 시간
    private Double latitude; // 지진 발생 위도
    private Double longitude; // 지진 발생 경도
    private Double depth; // 지진 깊이
} 
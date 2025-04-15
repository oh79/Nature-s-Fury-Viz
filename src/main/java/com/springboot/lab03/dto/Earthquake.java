package com.springboot.lab03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Earthquake {

    @JsonProperty("id") // JSON "id" 매핑
    private int id; // 지진 ID

    private int year; // 발생 년
    private int month; // 발생 월
    private int day; // 발생 일

    @JsonProperty("locationName") // JSON "locationName" 매핑
    private String location; // 위치명

    private double latitude; // 위도
    private double longitude; // 경도
    
    @JsonProperty("eqDepth") // JSON "eqDepth" 매핑
    private double depthKm; // 깊이 (km)

    @JsonProperty("eqMagnitude") // JSON "eqMagnitude" 매핑
    private Double magnitude; // 규모
} 
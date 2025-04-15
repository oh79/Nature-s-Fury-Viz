package com.springboot.lab03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 JSON 속성 무시
public class Volcano {

    @JsonProperty("id") // JSON "id" 매핑
    private int id; // 화산 ID

    private int year; // 발생 년
    private int month; // 발생 월
    private int day; // 발생 일
    private String name; // 화산 이름
    private String location; // 위치
    private String country; // 국가
    private double latitude; // 위도
    private double longitude; // 경도
    private int elevation; // 고도 (m)
    private int vei; // 화산 폭발 지수 (VEI)
} 
package com.springboot.lab03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 JSON 속성 무시
public class Tsunami {

    @JsonProperty("id") // JSON "id" 매핑
    private int id; // 쓰나미 ID

    private int year; // 발생 년
    private int month; // 발생 월
    private int day; // 발생 일

    @JsonProperty("eventValidity") // JSON "eventValidity" 매핑
    private int tsunamiEventValidity; // 이벤트 유효성 코드

    @JsonProperty("causeCode") // JSON "causeCode" 매핑
    private int tsunamiCauseCode; // 원인 코드

    private String country; // 국가

    @JsonProperty("locationName") // JSON "locationName" 매핑
    private String location; // 위치명

    private Double latitude; // 위도
    private Double longitude; // 경도

    @JsonProperty("maxWaterHeight") // JSON "maxWaterHeight" 매핑
    private Double maximumWaterHeight; // 최대 수위 (m)

    @JsonProperty("numRunups") // JSON "numRunups" 매핑
    private Integer numberOfRunup; // Runup 횟수
}

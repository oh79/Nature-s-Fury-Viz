package com.springboot.lab03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 속성은 무시
public class Tsunami {

    @JsonProperty("id") // API 응답의 id 필드 매핑
    private int id; // 상세보기를 위한 ID (int 타입으로 변경)

    private int year; // API: year
    private int month; // API: month
    private int day; // API: day

    @JsonProperty("eventValidity") // API: eventValidity 필드 매핑 (숫자형)
    private int tsunamiEventValidity; // 쓰나미 이벤트 유효성 (int 타입으로 변경)

    @JsonProperty("causeCode") // API: causeCode 필드 매핑 (숫자형)
    private int tsunamiCauseCode; // 쓰나미 원인 코드 (int 타입으로 변경)

    private String country; // API: country

    @JsonProperty("locationName") // API: locationName 필드를 location으로 매핑
    private String location;

    private Double latitude; // API: latitude
    private Double longitude; // API: longitude

    @JsonProperty("maxWaterHeight") // API: maxWaterHeight 필드 매핑
    private Double maximumWaterHeight; // 최대 수위 (미터 단위 추정)

    @JsonProperty("numRunups") // API: numRunups 필드 매핑
    private Integer numberOfRunup; // Runup 횟수
}

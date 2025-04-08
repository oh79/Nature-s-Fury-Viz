package com.springboot.lab02.model;

import lombok.Data;

@Data
public class Volcano {
    private String vnum;          // 화산 번호
    private String vName;         // 화산 이름
    private String country;       // 국가
    private String subregion;     // 세부 지역
    private Double latitude;      // 위도
    private Double longitude;     // 경도 
    private Integer elevationM;  // 고도(미터)
    private String obsAbbr;       // 관측소 약자
    private String webpage;       // 웹페이지 URL
} 
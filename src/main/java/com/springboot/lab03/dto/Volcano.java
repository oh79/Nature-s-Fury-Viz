package com.springboot.lab03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 속성은 무시
public class Volcano {

    @JsonProperty("id") // API 응답의 id 필드 매핑
    private int id; // 상세보기를 위한 ID (int 타입으로 변경)

    private int year;
    private int month;
    private int day;
    private String name; // API: name
    private String location; // API: location
    private String country; // API: country
    private double latitude; // API: latitude
    private double longitude; // API: longitude
    private int elevation; // API: elevation
    private int vei; // API: vei (Volcanic Explosivity Index)

    // 요청했던 'type' 필드는 API 응답에 직접적으로 없어서 제외함.
    // 필요하다면 'morphology'나 'status' 필드를 추가로 매핑할 수 있어.
} 
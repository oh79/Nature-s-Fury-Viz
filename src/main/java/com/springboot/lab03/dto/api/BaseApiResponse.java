package com.springboot.lab03.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * NOAA NCEI API 응답의 공통 페이징 구조를 위한 추상 DTO.
 * 제네릭 `T`는 실제 데이터 항목의 타입 (`Volcano`, `Earthquake` 등).
 *
 * `@Data`: Lombok (getter, setter 등 자동 생성)
 * `@JsonIgnoreProperties(ignoreUnknown = true)`: JSON 역직렬화 시 알 수 없는 속성 무시.
 *
 * @param <T> 데이터 항목 타입
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseApiResponse<T> {

    /**
     * 데이터 항목 리스트 (JSON: "items")
     */
    @JsonProperty("items")
    protected List<T> items;

    /**
     * 페이지당 항목 수 (JSON: "itemsPerPage")
     */
    @JsonProperty("itemsPerPage")
    protected int itemsPerPage;

    /**
     * 현재 페이지 번호 (JSON: "page")
     */
    @JsonProperty("page")
    protected int page;

    /**
     * 전체 항목 수 (JSON: "totalItems")
     */
    @JsonProperty("totalItems")
    protected long totalItems;

    /**
     * 전체 페이지 수 (JSON: "totalPages")
     */
    @JsonProperty("totalPages")
    protected int totalPages;

    // 하위 클래스에서 필요한 필드 추가 가능
} 
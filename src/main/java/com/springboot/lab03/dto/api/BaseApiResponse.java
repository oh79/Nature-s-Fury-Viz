package com.springboot.lab03.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * NOAA NCEI(National Centers for Environmental Information) API 응답의 공통적인 페이징 관련 구조를 나타내는 추상 클래스입니다.
 * 화산, 지진, 쓰나미 등 다양한 종류의 자연재해 데이터 목록 API 응답이 이 구조를 따릅니다.
 * 제네릭 타입 `T`는 실제 데이터 목록(`items`)에 포함될 구체적인 데이터 객체의 타입(예: `Volcano`, `Earthquake`, `Tsunami`)을 나타냅니다.
 *
 * `@Data`: Lombok 애노테이션으로, getter, setter, toString, equals, hashCode 메소드를 자동으로 생성합니다.
 * `@JsonIgnoreProperties(ignoreUnknown = true)`: Jackson 애노테이션으로, JSON 역직렬화 시 DTO 클래스에 정의되지 않은 속성이 있더라도 무시하고 처리합니다.
 *                                             이렇게 하면 API 응답에 예기치 않은 필드가 추가되더라도 애플리케이션 오류를 방지할 수 있습니다.
 *
 * @param <T> API 응답의 `items` 리스트에 포함될 데이터 요소의 타입 (e.g., Volcano, Earthquake, Tsunami)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseApiResponse<T> {

    /**
     * 현재 페이지에 해당하는 실제 데이터 객체들의 리스트입니다.
     * JSON 응답의 "items" 필드와 매핑됩니다.
     * 리스트의 요소 타입은 제네릭 파라미터 `T`에 의해 결정됩니다.
     */
    @JsonProperty("items") // JSON 응답의 "items" 필드를 이 필드에 매핑합니다.
    protected List<T> items;

    /**
     * 페이지당 표시되는 항목(데이터)의 수입니다.
     * JSON 응답의 "itemsPerPage" 필드와 매핑됩니다.
     */
    @JsonProperty("itemsPerPage") // JSON 응답의 "itemsPerPage" 필드를 이 필드에 매핑합니다.
    protected int itemsPerPage;

    /**
     * 현재 페이지 번호입니다. API 응답에 따라 0 또는 1부터 시작할 수 있습니다.
     * (NOAA NCEI API는 일반적으로 1부터 시작하는 것으로 보입니다.)
     * JSON 응답의 "page" 필드와 매핑됩니다.
     */
    @JsonProperty("page") // JSON 응답의 "page" 필드를 이 필드에 매핑합니다.
    protected int page;

    /**
     * 전체 검색 결과에 해당하는 총 항목(데이터)의 수입니다.
     * JSON 응답의 "totalItems" 필드와 매핑됩니다.
     */
    @JsonProperty("totalItems") // JSON 응답의 "totalItems" 필드를 이 필드에 매핑합니다.
    protected long totalItems;

    /**
     * 전체 검색 결과에 대한 총 페이지 수입니다.
     * `totalItems`와 `itemsPerPage`를 기반으로 계산된 값입니다.
     * JSON 응답의 "totalPages" 필드와 매핑됩니다.
     */
    @JsonProperty("totalPages") // JSON 응답의 "totalPages" 필드를 이 필드에 매핑합니다.
    protected int totalPages;

    // 상속받는 하위 클래스(e.g., VolcanoApiResponse)에서 필요에 따라 추가적인 필드를 정의할 수 있습니다.
} 
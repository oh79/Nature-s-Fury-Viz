package com.springboot.lab03.dto.api;

import com.springboot.lab03.dto.Earthquake;
import lombok.Data;

/**
 * NOAA NCEI API의 지진 데이터 목록 조회 응답을 위한 DTO(Data Transfer Object) 클래스입니다.
 * 공통 페이징 정보는 {@link BaseApiResponse} 클래스로부터 상속받습니다.
 * 제네릭 타입 파라미터로 {@link Earthquake}를 지정하여 `items` 리스트가 지진 데이터 객체를 포함하도록 합니다.
 *
 * `@Data`: Lombok 애노테이션으로, getter, setter, toString, equals, hashCode 메소드를 자동으로 생성합니다.
 */
@Data
public class EarthquakeApiResponse extends BaseApiResponse<Earthquake> {

    // BaseApiResponse<Earthquake> 를 상속받음으로써,
    // protected List<Earthquake> items;
    // protected int itemsPerPage;
    // protected int page;
    // protected long totalItems;
    // protected int totalPages;
    // 위 필드들을 포함하게 됩니다.

    // 만약 지진 API 응답에만 특별히 존재하는 페이징 외의 추가적인 최상위 레벨 필드가 있다면,
    // 여기에 해당 필드를 선언하고 @JsonProperty 애노테이션으로 JSON 필드 이름을 매핑할 수 있습니다.
    // 예시: @JsonProperty("specificEarthquakeField") private String specificField;
    // 현재 NOAA NCEI API 응답 구조에서는 페이징 관련 필드 외에 특별한 최상위 필드는 보이지 않습니다.
} 
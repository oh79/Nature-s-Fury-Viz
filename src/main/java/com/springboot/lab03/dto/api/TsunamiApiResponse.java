package com.springboot.lab03.dto.api;

import com.springboot.lab03.dto.Tsunami;

import lombok.Data;

/**
 * NOAA NCEI 지진 데이터 API 응답 DTO.
 * {@link BaseApiResponse}를 상속받아 페이징 정보 포함.
 * `@Data`: Lombok 애노테이션 (getter, setter 등 자동 생성)
 */
@Data
public class TsunamiApiResponse extends BaseApiResponse<Tsunami> {

} 
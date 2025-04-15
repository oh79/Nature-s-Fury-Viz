package com.springboot.lab03.service;

import com.springboot.lab03.dto.Earthquake;

/**
 * 지진 데이터 서비스 인터페이스.
 * {@link DataService}를 상속받아 공통 CRUD 메소드를 정의.
 */
public interface EarthquakeService extends DataService<Earthquake> {
    // 추가적인 지진 서비스 메소드 선언
}
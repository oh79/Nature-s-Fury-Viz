package com.springboot.lab03.service;

import com.springboot.lab03.dto.Volcano;

/**
 * 화산 데이터 서비스 인터페이스.
 * {@link DataService}를 상속받아 공통 CRUD 메소드를 정의.
 */
public interface VolcanoService extends DataService<Volcano> {
    // 추가적인 화산 서비스 메소드 선언
} 
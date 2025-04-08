package com.springboot.lab02.service;

import java.util.List;

/**
 * 자연재해 데이터 서비스 인터페이스
 * - 의존성 역전 원칙(DIP)을 적용하여 추상화에 의존하도록 함
 */
public interface DataService<T> {
    
    /**
     * 모든 데이터를 가져옵니다.
     * @return 데이터 목록
     */
    List<T> getAllData();
    
    /**
     * ID로 특정 데이터를 가져옵니다.
     * @param id 데이터 ID
     * @return 데이터 객체
     */
    T getDataById(String id);
} 
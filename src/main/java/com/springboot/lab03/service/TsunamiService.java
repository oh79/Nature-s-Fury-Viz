package com.springboot.lab03.service;

import com.springboot.lab03.dto.Tsunami;

/**
 * 쓰나미 데이터 서비스 인터페이스.
 * {@link DataService}를 상속받아 공통 CRUD 메소드를 정의.
 */
public interface TsunamiService extends DataService<Tsunami> {
    // 추가적인 쓰나미 서비스 메소드 선언
}

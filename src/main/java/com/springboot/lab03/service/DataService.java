package com.springboot.lab03.service;

import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;

/**
 * 자연재해 데이터 서비스 공통 인터페이스
 */
public interface DataService<T> {

    /**
     * 지정된 검색 조건에 맞는 데이터 목록을 가져옵니다.
     *
     * @param criteria 검색 조건 객체 (페이징, 날짜, 위치, 키워드 등 포함)
     * @return 페이징된 데이터 목록 (PageDTO)
     */
    PageDTO<T> getData(SearchCriteria criteria);

    /**
     * ID로 특정 데이터를 가져옵니다. (상세보기)
     * API 명세상 상세보기 엔드포인트가 별도로 있으므로, 이 메서드도 유지합니다.
     *
     * @param id 데이터 ID (int 타입으로 변경됨)
     * @return 데이터 객체
     */
    T getDataById(int id); // ID 타입을 int로 변경
} 
package com.springboot.lab03.service;

import com.springboot.lab03.dto.Earthquake;
import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;

/**
 * 지진 데이터 서비스 인터페이스
 */
public interface EarthquakeService {
    PageDTO<Earthquake> getData(SearchCriteria criteria);
    Earthquake getDataById(int id);
}
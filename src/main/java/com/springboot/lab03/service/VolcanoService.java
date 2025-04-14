package com.springboot.lab03.service;

import com.springboot.lab03.dto.Volcano;
import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;

/**
 * 화산 데이터 서비스 인터페이스
 */
public interface VolcanoService {
    PageDTO<Volcano> getData(SearchCriteria criteria);
    Volcano getDataById(int id);
} 
package com.springboot.lab03.service;

import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;
import com.springboot.lab03.dto.Tsunami;
import java.util.List;

/**
 * 쓰나미 데이터 서비스 인터페이스
 */
public interface TsunamiService {
    PageDTO<Tsunami> getData(SearchCriteria criteria);
    Tsunami getDataById(int id);
}

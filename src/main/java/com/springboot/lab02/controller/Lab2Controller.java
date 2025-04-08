package com.springboot.lab02.controller;

import com.springboot.lab02.service.EarthquakeService;
import com.springboot.lab02.service.VolcanoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

/**
 * Lab2 데이터 목록 컨트롤러
 * - 서버 사이드 페이징 적용 (In-Memory)
 */
@Slf4j
@Controller
@RequestMapping("/lab2")
public class Lab2Controller {
    
    private final VolcanoService volcanoService; 
    private final EarthquakeService earthquakeService;
    
    public Lab2Controller(VolcanoService volcanoService, EarthquakeService earthquakeService) {
        this.volcanoService = volcanoService;
        this.earthquakeService = earthquakeService;
    }
    
    /**
     * Lab2 데이터 목록 페이지를 표시합니다.
     * 
     * @param type 데이터 유형 (volcano, earthquake)
     * @param page 요청 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @param model Spring 모델
     * @return lab2 뷰 이름
     */
    @GetMapping("/{type}")
    public String showLab2(@PathVariable String type,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        
        size = Math.max(1, Math.min(size, 50)); 
        
        Sort sort = Sort.unsorted();
        Page<?> pageData = null; // pageData 초기화

        if ("volcano".equalsIgnoreCase(type)) {
            sort = Sort.by(Sort.Direction.ASC, "vName");
            Pageable pageable = PageRequest.of(page, size, sort);
            pageData = volcanoService.getPagedVolcanoes(pageable); // 직접 호출
        } else if ("earthquake".equalsIgnoreCase(type)) {
            sort = Sort.by(Sort.Direction.DESC, "time");
            Pageable pageable = PageRequest.of(page, size, sort);
            pageData = earthquakeService.getPagedEarthquakes(pageable); // 직접 호출
        } else {
             // 유효하지 않은 타입 처리
             log.warn("Invalid data type requested: {}", type);
             pageData = Page.empty(PageRequest.of(page, size)); // 빈 페이지 반환
             model.addAttribute("error", "잘못된 데이터 타입입니다.");
        }
        
        if (pageData != null) { // pageData가 null이 아닌 경우에만 로그 기록
            log.info("Lab2 페이지 요청: type={}, page={}, size={}, 총 항목 수={}, 총 페이지 수={}", 
                    type, page, size, pageData.getTotalElements(), pageData.getTotalPages());
        }
        
        model.addAttribute("activeTab", type);
        model.addAttribute("pageData", pageData);
        model.addAttribute("title", type.equals("volcano") ? "화산 데이터 목록" : "지진 데이터 목록"); // 지도 문구 제거
        
        // 데이터 로딩 실패 시 오류 처리 (pageData가 null 이거나 content가 비었을 때)
        if (pageData == null || pageData.getContent().isEmpty()) { 
             boolean isCacheEmpty = false;
             if ("volcano".equalsIgnoreCase(type)) {
                 isCacheEmpty = volcanoService.getAllData().isEmpty();
             } else if ("earthquake".equalsIgnoreCase(type)) {
                 isCacheEmpty = earthquakeService.getAllData().isEmpty();
             }

             if (isCacheEmpty) {
                 // model.addAttribute("error", "데이터를 불러올 수 없습니다. API 서버 상태를 확인하거나 잠시 후 다시 시도해주세요."); // 이미 위에서 처리
                 log.error("{} 데이터를 가져올 수 없음 (캐시 비어있음, API 실패 가능성)", type);
             } else if (pageData != null && pageData.getTotalElements() > 0 && pageData.getContent().isEmpty()) {
                 log.warn("요청한 페이지({})에 {} 데이터가 없습니다. (총 {}개 항목 존재)", page, type, pageData.getTotalElements());
             } else if (pageData != null) {
                 log.info("표시할 {} 데이터가 없습니다.", type);
             }
        }
        
        return "lab2";
    }
} 
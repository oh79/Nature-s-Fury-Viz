package com.springboot.lab03.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;
import com.springboot.lab03.service.EarthquakeService;
import com.springboot.lab03.service.TsunamiService;
import com.springboot.lab03.service.VolcanoService;

/**
 * 자연재해 데이터(화산, 지진, 쓰나미) 목록 조회/검색 컨트롤러.
 * `/lab3/{type}` 경로 요청 처리.
 */
@Controller
@RequestMapping("/lab3") // 컨트롤러 기본 경로
public class Lab3Controller {

    // 각 데이터 서비스 주입
    private final VolcanoService volcanoService;
    private final EarthquakeService earthquakeService;
    private final TsunamiService tsunamiService;

    /**
     * 생성자를 통한 서비스 의존성 주입.
     *
     * @param volcanoService 화산 서비스
     * @param earthquakeService 지진 서비스
     * @param tsunamiService 쓰나미 서비스
     */
    public Lab3Controller(VolcanoService volcanoService,
                          EarthquakeService earthquakeService,
                          TsunamiService tsunamiService) {
        this.volcanoService = volcanoService;
        this.earthquakeService = earthquakeService;
        this.tsunamiService = tsunamiService;
    }

    /**
     * 데이터 목록 페이지 표시.
     *
     * @param type URL 경로의 데이터 유형 ("volcano", "earthquake", "tsunami")
     * @param criteria HTTP 요청 파라미터를 바인딩한 검색 조건 객체 (@ModelAttribute)
     * @param model 뷰에 전달할 데이터 모델
     * @return 렌더링할 뷰 이름 ("lab3")
     */
    @GetMapping("/{type}") // GET /lab3/{type} 매핑
    public String showList(@PathVariable String type,
                           @ModelAttribute SearchCriteria criteria,
                           Model model) {

        // 페이지당 항목 수 제한 (1~50)
        criteria.setSize(Math.max(1, Math.min(criteria.getSize(), 50)));

        PageDTO<?> pageData = null; // 페이징 데이터 결과
        String title = "데이터 목록"; // 페이지 제목

        try {
            // 타입별 서비스 호출
            if ("volcano".equalsIgnoreCase(type)) {
                title = "화산 데이터 목록";
                pageData = volcanoService.getData(criteria);
            } else if ("earthquake".equalsIgnoreCase(type)) {
                title = "지진 데이터 목록";
                pageData = earthquakeService.getData(criteria);
            } else if ("tsunami".equalsIgnoreCase(type)) {
                title = "쓰나미 데이터 목록";
                pageData = tsunamiService.getData(criteria);
            } else {
                // 잘못된 타입 처리
                model.addAttribute("error", "잘못된 데이터 타입: " + type);
                pageData = new PageDTO<>(); // 빈 결과
            }
        } catch (Exception e) {
             // 서비스 예외 처리
             model.addAttribute("error", "데이터 조회 중 오류 발생. 잠시 후 다시 시도해주세요.");
             pageData = new PageDTO<>(); // 빈 결과
        }

        // 모델에 데이터 추가
        model.addAttribute("activeTab", type);       // 활성 탭
        model.addAttribute("pageData", pageData);     // 페이징 데이터
        model.addAttribute("title", title);          // 페이지 제목
        model.addAttribute("criteria", criteria);    // 검색 조건 (폼 유지용)

        // 결과 없음 메시지 처리
        if (pageData != null && pageData.getTotalItems() == 0 && !model.containsAttribute("error")) {
            if ("volcano".equalsIgnoreCase(type) || "earthquake".equalsIgnoreCase(type) || "tsunami".equalsIgnoreCase(type)) {
                 model.addAttribute("message", "검색 결과가 없습니다.");
            }
        }

        return "lab3"; // lab3.html 뷰 반환
    }
} 
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

import lombok.extern.slf4j.Slf4j;

/**
 * 자연재해(화산, 지진, 쓰나미) 데이터의 목록 조회 및 검색 기능을 처리하는 메인 컨트롤러입니다.
 * 사용자가 선택한 데이터 유형(type)과 검색 조건(criteria)에 따라 해당 데이터를 조회하고,
 * 페이징 처리된 결과를 `lab3.html` 템플릿을 통해 사용자에게 보여줍니다.
 * URL 경로 `/lab3/{type}` 형태로 요청을 받습니다. (예: `/lab3/volcano`, `/lab3/earthquake?page=2&minDate=2023-01-01`)
 */
@Slf4j // Lombok: 로깅을 위한 log 객체 자동 생성
@Controller // 스프링 MVC 컨트롤러로 지정
@RequestMapping("/lab3") // 이 컨트롤러의 모든 핸들러 메소드에 대한 기본 URL 경로 접두사
public class Lab3Controller {

    // 각 데이터 유형별 서비스를 주입받기 위한 final 필드
    private final VolcanoService volcanoService;
    private final EarthquakeService earthquakeService;
    private final TsunamiService tsunamiService;

    /**
     * 생성자를 통한 서비스 의존성 주입.
     * 스프링 컨테이너가 관리하는 각 서비스의 구현체 빈(Bean)을 자동으로 주입합니다.
     *
     * @param volcanoService 화산 데이터 서비스
     * @param earthquakeService 지진 데이터 서비스
     * @param tsunamiService 쓰나미 데이터 서비스
     */
    public Lab3Controller(VolcanoService volcanoService,
                          EarthquakeService earthquakeService,
                          TsunamiService tsunamiService) {
        this.volcanoService = volcanoService;
        this.earthquakeService = earthquakeService;
        this.tsunamiService = tsunamiService;
    }

    /**
     * 지정된 데이터 유형(`type`)과 검색 조건(`criteria`)에 따라 데이터 목록 페이지를 표시하는 요청 핸들러 메소드입니다.
     *
     * @param type URL 경로 변수({type})로부터 받은 데이터 유형 문자열 (e.g., "volcano", "earthquake", "tsunami")
     * @param criteria HTTP 요청 파라미터들을 자동으로 바인딩한 {@link SearchCriteria} 객체.
     *                 요청 파라미터 이름과 SearchCriteria 필드 이름이 일치하면 자동으로 값이 설정됩니다.
     *                 (예: `?page=2&minDate=2023-01-01` 요청 -> criteria 객체의 page=2, minDate="2023-01-01" 설정)
     *                 `@ModelAttribute`는 이 파라미터가 모델에도 자동으로 추가되도록 합니다 (뷰에서 `criteria` 이름으로 접근 가능).
     * @param model 뷰(템플릿)에 데이터를 전달하기 위한 Spring UI Model 객체
     * @return 렌더링할 뷰의 논리적인 이름 ("lab3"). `lab3.html` 템플릿을 사용합니다.
     */
    @GetMapping("/{type}") // HTTP GET 요청과 "/lab3/{type}" 패턴의 URL을 매핑합니다.
    public String showList(@PathVariable String type,
                           @ModelAttribute SearchCriteria criteria,
                           Model model) {

        // 페이지당 항목 수(size) 기본값 및 최대/최소 제한 설정 (1 ~ 50개 사이)
        criteria.setSize(Math.max(1, Math.min(criteria.getSize(), 50)));

        log.info("Lab3 목록 페이지 요청: type={}, criteria={}", type, criteria); // 요청 정보 로깅

        PageDTO<?> pageData = null; // 페이징된 데이터 결과를 담을 변수 (초기값 null)
        String title = "데이터 목록"; // 페이지 제목 기본값

        try {
            // 요청된 데이터 유형(type)에 따라 적절한 서비스를 호출하여 데이터를 조회합니다.
            if ("volcano".equalsIgnoreCase(type)) { // "volcano" (대소문자 무시)
                title = "화산 데이터 목록"; // 페이지 제목 설정
                pageData = volcanoService.getData(criteria); // 화산 서비스 호출
            } else if ("earthquake".equalsIgnoreCase(type)) { // "earthquake"
                title = "지진 데이터 목록";
                pageData = earthquakeService.getData(criteria); // 지진 서비스 호출
            } else if ("tsunami".equalsIgnoreCase(type)) { // "tsunami"
                title = "쓰나미 데이터 목록";
                pageData = tsunamiService.getData(criteria); // 쓰나미 서비스 호출
            } else {
                // 지원하지 않는 데이터 타입이 요청된 경우
                log.warn("유효하지 않은 데이터 타입 요청: {}", type);
                model.addAttribute("error", "잘못된 데이터 타입입니다: " + type); // 오류 메시지를 모델에 추가
                pageData = new PageDTO<>(); // 빈 PageDTO 객체 생성 (결과 없음 처리)
            }
        } catch (Exception e) {
             // 데이터 조회 중 예외 발생 시 (API 호출 실패 등)
             log.error("{} 데이터 조회 중 오류 발생: {}", type, e.getMessage(), e); // 오류 로그 기록 (스택 트레이스 포함)
             model.addAttribute("error", "데이터를 조회하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."); // 사용자 친화적 오류 메시지
             pageData = new PageDTO<>(); // 빈 PageDTO 객체 생성
        }

        // 뷰(lab3.html)에 전달할 데이터들을 Model 객체에 추가합니다.
        model.addAttribute("activeTab", type);       // 현재 활성화된 탭(데이터 유형) 정보
        model.addAttribute("pageData", pageData);     // 페이징된 데이터 결과 (PageDTO)
        model.addAttribute("title", title);          // 페이지 제목
        model.addAttribute("criteria", criteria);    // 현재 사용된 검색 조건 (폼 유지 및 페이징 링크 생성에 사용)

        // 조회된 데이터가 없고, 오류 메시지도 없는 경우 (정상적으로 조회했으나 결과가 없는 경우)
        if (pageData != null && pageData.getTotalItems() == 0 && !model.containsAttribute("error")) {
            // 유효한 데이터 타입 요청이었을 때만 "결과 없음" 메시지 표시
            if ("volcano".equalsIgnoreCase(type) || "earthquake".equalsIgnoreCase(type) || "tsunami".equalsIgnoreCase(type)) {
                 log.info("요청 조건에 맞는 {} 데이터가 없습니다.", type);
                 model.addAttribute("message", "검색 조건에 맞는 데이터가 없습니다."); // 정보 메시지를 모델에 추가
            }
        }

        return "lab3"; // "lab3.html" 템플릿을 렌더링하도록 뷰 이름을 반환
    }
} 
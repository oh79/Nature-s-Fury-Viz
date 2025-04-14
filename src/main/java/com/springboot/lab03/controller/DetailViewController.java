package com.springboot.lab03.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.springboot.lab03.dto.Earthquake;
import com.springboot.lab03.dto.Tsunami;
import com.springboot.lab03.dto.Volcano;
import com.springboot.lab03.service.EarthquakeService;
import com.springboot.lab03.service.TsunamiService;
import com.springboot.lab03.service.VolcanoService;

import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * 상세 정보 뷰 컨트롤러
 * - 상세 정보 페이지(detailView.html) 렌더링 담당
 */
@Slf4j
@Controller
@RequestMapping("/lab3/detail")
public class DetailViewController {
    
    private final VolcanoService volcanoService;
    private final EarthquakeService earthquakeService;
    private final TsunamiService tsunamiService;
    
    public DetailViewController(VolcanoService volcanoService, EarthquakeService earthquakeService, TsunamiService tsunamiService) {
        this.volcanoService = volcanoService;
        this.earthquakeService = earthquakeService;
        this.tsunamiService = tsunamiService;
    }
    
    /**
     * 화산 상세 정보 페이지를 표시합니다.
     * 
     * @param id 화산 ID
     * @param model Spring 모델
     * @return 상세 정보 뷰 이름 ("detailView")
     */
    @GetMapping("/volcano/{id}")
    public String getVolcanoDetail(@PathVariable("id") int id, Model model) {
        log.info("화산 상세 정보 페이지 요청: ID={}", id);
        Volcano volcano = volcanoService.getDataById(id);
        
        if (volcano == null) {
            log.warn("화산 데이터를 찾을 수 없음: ID={}", id);
            model.addAttribute("error", "요청하신 화산 정보를 찾을 수 없습니다.");
            model.addAttribute("title", "데이터 없음");
        } else {
            log.info("화산 데이터 로드 성공: {}", volcano.getName());
            model.addAttribute("volcano", volcano);
            model.addAttribute("title", volcano.getName() != null ? volcano.getName() + " 상세 정보" : "화산 상세 정보");
        }
        
        return "detailView";
    }
    
    /**
     * 지진 상세 정보 페이지를 표시합니다.
     * 
     * @param id 지진 ID
     * @param model Spring 모델
     * @return 상세 정보 뷰 이름 ("detailView")
     */
    @GetMapping("/earthquake/{id}")
    public String getEarthquakeDetail(@PathVariable("id") int id, Model model) {
        log.info("지진 상세 정보 페이지 요청: ID={}", id);
        Earthquake earthquake = earthquakeService.getDataById(id);
        
        if (earthquake == null) {
            log.warn("지진 데이터를 찾을 수 없음: ID={}", id);
            model.addAttribute("error", "요청하신 지진 정보를 찾을 수 없습니다.");
            model.addAttribute("title", "데이터 없음");
        } else {
            log.info("지진 데이터 로드 성공: 규모 {}", earthquake.getMagnitude());
            model.addAttribute("earthquake", earthquake);
            String title = "지진 상세 정보";
            if (earthquake.getMagnitude() != null) {
                 title = "규모 " + earthquake.getMagnitude() + " 지진 상세 정보";
            }
            model.addAttribute("title", title);
        }
        
        return "detailView";
    }

    /**
     * 쓰나미 상세 정보 페이지
     * @param id 쓰나미 ID
     */
    @GetMapping("/tsunami/{id}")
    public String getTsunamiDetail(@PathVariable("id") int id, Model model) {
        log.info("쓰나미 상세 정보 페이지 요청: ID={}", id);
        Tsunami tsunami = tsunamiService.getDataById(id);

        if (tsunami == null) {
            log.warn("쓰나미 데이터를 찾을 수 없음: ID={}", id);
            model.addAttribute("error", "요청하신 쓰나미 정보를 찾을 수 없습니다.");
            model.addAttribute("title", "데이터 없음");
        } else {
            log.info("쓰나미 데이터 로드 성공: 지역 {}", tsunami.getLocation());
            model.addAttribute("tsunami", tsunami);
            model.addAttribute("title", tsunami.getLocation() != null ? tsunami.getLocation() + " 쓰나미 상세 정보" : "쓰나미 상세 정보");
        }
        return "detailView";
    }

    // --- 예외 처리 ---
    @ExceptionHandler(NumberFormatException.class)
    public String handleNumberFormatError(NumberFormatException ex, Model model) {
        log.warn("잘못된 ID 형식 요청: {}", ex.getMessage());
        model.addAttribute("error", "ID는 숫자 형식이어야 합니다.");
        model.addAttribute("title", "잘못된 요청");
        return "detailView";
    }
} 
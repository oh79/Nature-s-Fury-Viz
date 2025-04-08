package com.springboot.lab02.controller;

import com.springboot.lab02.model.Earthquake;
import com.springboot.lab02.model.Volcano;
import com.springboot.lab02.service.DataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * 상세 정보 뷰 컨트롤러
 * - 상세 정보 페이지(detailView.html) 렌더링 담당
 */
@Slf4j
@Controller
@RequestMapping("/detail")
public class DetailViewController {
    
    private final DataService<Volcano> volcanoService;
    private final DataService<Earthquake> earthquakeService;
    
    public DetailViewController(DataService<Volcano> volcanoService, DataService<Earthquake> earthquakeService) {
        this.volcanoService = volcanoService;
        this.earthquakeService = earthquakeService;
    }
    
    /**
     * 화산 상세 정보 페이지를 표시합니다.
     * 
     * @param id 화산 ID
     * @param model Spring 모델
     * @return 상세 정보 뷰 이름 ("detailView")
     */
    @GetMapping("/volcano/{id}")
    public String getVolcanoDetail(@PathVariable("id") String id, Model model) {
        log.info("화산 상세 정보 페이지 요청: ID={}", id);
        Volcano volcano = volcanoService.getDataById(id);
        
        if (volcano == null) {
            log.warn("화산 데이터를 찾을 수 없음: ID={}", id);
            model.addAttribute("error", "요청하신 화산 정보를 찾을 수 없습니다. ID를 확인해주세요.");
            model.addAttribute("title", "데이터 없음");
        } else {
            log.info("화산 데이터 로드 성공: {}", volcano.getVName());
            model.addAttribute("volcano", volcano);
            model.addAttribute("title", volcano.getVName() != null ? volcano.getVName() + " 상세 정보" : "화산 상세 정보");
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
    public String getEarthquakeDetail(@PathVariable("id") String id, Model model) {
        log.info("지진 상세 정보 페이지 요청: ID={}", id);
        Earthquake earthquake = earthquakeService.getDataById(id);
        
        if (earthquake == null) {
            log.warn("지진 데이터를 찾을 수 없음: ID={}", id);
            model.addAttribute("error", "요청하신 지진 정보를 찾을 수 없습니다. ID를 확인해주세요.");
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
} 
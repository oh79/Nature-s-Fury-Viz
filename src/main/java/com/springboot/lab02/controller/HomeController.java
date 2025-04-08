package com.springboot.lab02.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * 홈 컨트롤러
 * - 단일 책임 원칙(SRP): 메인 페이지와 리다이렉션만 담당
 */
@Controller
@Slf4j
public class HomeController {

    /**
     * 메인 페이지를 처리합니다.
     * "/" 경로로 접속할 경우 "/lab2/volcano"로 리다이렉트합니다.
     * @return 리다이렉트 경로
     */
    @GetMapping("/")
    public String home() {
        log.info("홈 페이지 요청: /lab2/volcano로 리다이렉트");
        return "redirect:/lab2/volcano";
    }
}
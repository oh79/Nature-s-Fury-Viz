package com.springboot.lab03.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * 애플리케이션의 루트 경로("/") 요청을 처리하는 컨트롤러입니다.
 * 사용자가 웹 애플리케이션의 기본 주소로 접속했을 때 초기 페이지를 결정하는 역할을 합니다.
 * 현재 구현에서는 기본적으로 화산 데이터 목록 페이지(`/lab3/volcano`)로 리다이렉트시킵니다.
 */
@Controller // 이 클래스가 스프링 MVC의 컨트롤러임을 나타냅니다.
@Slf4j // Lombok 애노테이션: 로깅을 위한 Logger 객체(log)를 자동으로 생성합니다.
public class HomeController {

    /**
     * HTTP GET 요청으로 애플리케이션의 루트 경로("/")가 호출되었을 때 실행되는 핸들러 메소드입니다.
     * "/lab3/volcano" 경로로 클라이언트(브라우저)를 리다이렉트시키는 응답을 반환합니다.
     *
     * @return 리다이렉트할 URL 경로 문자열 ("redirect:/lab3/volcano").
     *         "redirect:" 접두사는 스프링 MVC에게 이 문자열이 뷰 이름이 아니라
     *         클라이언트에게 HTTP 302 리다이렉트 응답을 보내야 함을 알려줍니다.
     */
    @GetMapping("/") // HTTP GET 요청과 "/" 경로를 이 메소드에 매핑합니다.
    public String home() {
        log.info("홈 페이지 요청: /lab3/volcano 로 리다이렉트"); // 리다이렉트 수행 전 로그 기록
        return "redirect:/lab3/volcano"; // 화산 목록 페이지로 리다이렉트
    }
}
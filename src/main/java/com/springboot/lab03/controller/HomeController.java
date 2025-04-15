package com.springboot.lab03.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 경로("/") 요청을 `/lab3/volcano`로 리다이렉트하는 컨트롤러.
 */
@Controller
public class HomeController {

    /**
     * 루트 경로("/") GET 요청을 받아 `/lab3/volcano`로 리다이렉트.
     *
     * @return 리다이렉트 경로 문자열 ("redirect:/lab3/volcano")
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/lab3/volcano";
    }
}
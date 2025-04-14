package com.springboot.lab03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 애플리케이션의 빈 설정을 관리하는 클래스
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate 빈을 등록합니다.
     * 서비스 계층에서 외부 API를 호출하는 데 사용됩니다.
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 
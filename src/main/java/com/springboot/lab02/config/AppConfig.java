package com.springboot.lab02.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * 애플리케이션의 빈 설정을 관리하는 클래스
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate 빈을 등록합니다.
     * 서비스 계층에서 외부 API를 호출하는 데 사용됩니다.
     * HTML로 오는 JSON 응답을 처리하기 위한 Converter를 포함합니다.
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // HTML로 오는 JSON 응답을 처리하기 위한 Converter 설정
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // TEXT_HTML 미디어 타입도 JSON으로 처리하도록 설정
        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON, 
                MediaType.TEXT_HTML
        ));
        
        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }
} 
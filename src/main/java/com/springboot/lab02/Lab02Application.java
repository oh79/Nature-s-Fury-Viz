package com.springboot.lab02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// DataSource 자동 설정을 제외하여 In-Memory 방식으로 동작하도록 함
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Lab02Application {	

	public static void main(String[] args) {
		SpringApplication.run(Lab02Application.class, args);
	}
}

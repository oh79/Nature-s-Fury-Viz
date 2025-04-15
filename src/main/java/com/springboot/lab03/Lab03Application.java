package com.springboot.lab03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Lab03Application {	

	public static void main(String[] args) {
		SpringApplication.run(Lab03Application.class, args);
	}
}

package com.scb.askopt_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.scb.askopt_backend.mapper")
public class AskopsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AskopsBackendApplication.class, args);
	}

}

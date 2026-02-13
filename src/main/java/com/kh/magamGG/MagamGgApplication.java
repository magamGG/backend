package com.kh.magamGG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 스케줄러 활성화 (만료 토큰 정리용)
public class MagamGgApplication {

	public static void main(String[] args) {
		SpringApplication.run(MagamGgApplication.class, args);
	}

}

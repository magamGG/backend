package com.kh.magamGG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MagamGgApplication {

	public static void main(String[] args) {
		SpringApplication.run(MagamGgApplication.class, args);
	}

}

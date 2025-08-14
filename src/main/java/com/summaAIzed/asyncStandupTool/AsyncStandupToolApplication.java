package com.summaAIzed.asyncStandupTool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AsyncStandupToolApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsyncStandupToolApplication.class, args);
	}

}

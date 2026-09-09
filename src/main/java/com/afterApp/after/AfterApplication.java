package com.afterApp.after;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AfterApplication {

	public static void main(String[] args) {
		SpringApplication.run(AfterApplication.class, args);
	}
}

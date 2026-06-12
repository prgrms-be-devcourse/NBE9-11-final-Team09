package com.back.team9.moyeota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoyeotaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoyeotaApplication.class, args);
	}

}

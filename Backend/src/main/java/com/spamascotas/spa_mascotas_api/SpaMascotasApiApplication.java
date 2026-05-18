package com.spamascotas.spa_mascotas_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpaMascotasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpaMascotasApiApplication.class, args);
	}

}

package com.creditienda.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@ComponentScan(basePackages = { "com.creditienda.demo", "com.creditienda.controller", "com.creditienda.service" })
public class DemoApplication {

	@Value("${config.source:DEFAULT}")
	private String configSource;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@PostConstruct
	public void logConfigSource() {
		System.out.println("🔍 Configuración cargada desde: " + configSource);
	}
}

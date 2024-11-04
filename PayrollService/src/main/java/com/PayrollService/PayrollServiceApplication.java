package com.PayrollService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class PayrollServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayrollServiceApplication.class, args);
	}

}

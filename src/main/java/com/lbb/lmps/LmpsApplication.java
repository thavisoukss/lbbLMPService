package com.lbb.lmps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LmpsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmpsApplication.class, args);
		System.out.println("********* LMPS Service Start *********");
	}

}

package com.saurab.atomicledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtomicLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtomicLedgerApplication.class, args);
	}

}

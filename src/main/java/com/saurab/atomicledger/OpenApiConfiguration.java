package com.saurab.atomicledger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {

	@Bean
	public OpenAPI atomicLedgerOpenApi() {
		return new OpenAPI().info(new Info()
			.title("AtomicLedger API")
			.description("Transaction-safe wallet and double-entry ledger backend")
			.version("v1"));
	}
}

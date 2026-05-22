package com.saurab.atomicledger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
@SecurityScheme(
	name = "apiKeyAuth",
	type = SecuritySchemeType.APIKEY,
	in = SecuritySchemeIn.HEADER,
	paramName = "X-API-Key",
	description = "API key required for AtomicLedger API operations under /api/v1/**."
)
public class OpenApiConfiguration {

	@Bean
	public OpenAPI atomicLedgerOpenApi() {
		return new OpenAPI().info(new Info()
			.title("AtomicLedger API")
			.description("Transaction-safe wallet and double-entry ledger backend")
			.version("v1"));
	}

	@Bean
	public OpenApiCustomizer protectedApiSecurityCustomizer() {
		return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
			if (!path.startsWith("/api/v1/")) {
				return;
			}
			SecurityRequirement requirement = new SecurityRequirement().addList("apiKeyAuth");
			pathItem.readOperations().forEach(operation -> operation.addSecurityItem(requirement));
		});
	}
}

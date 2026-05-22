package com.saurab.atomicledger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.saurab.atomicledger.wallet.api.ApiErrorResponseFactory;

@Configuration
public class ApiKeySecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.requestCache(requestCache -> requestCache.disable())
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health").permitAll()
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().permitAll())
			.addFilterBefore(apiKeyAuthenticationFilter, AnonymousAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
		@Value("${atomicledger.security.api-key}") String configuredApiKey,
		ApiErrorResponseFactory apiErrorResponseFactory
	) {
		return new ApiKeyAuthenticationFilter(configuredApiKey, apiErrorResponseFactory);
	}
}

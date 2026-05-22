package com.saurab.atomicledger;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.saurab.atomicledger.wallet.api.ApiErrorDetailResponse;
import com.saurab.atomicledger.wallet.api.ApiErrorResponseFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	static final String API_KEY_HEADER = "X-API-Key";

	private final String configuredApiKey;
	private final ApiErrorResponseFactory apiErrorResponseFactory;

	public ApiKeyAuthenticationFilter(String configuredApiKey, ApiErrorResponseFactory apiErrorResponseFactory) {
		this.configuredApiKey = configuredApiKey;
		this.apiErrorResponseFactory = apiErrorResponseFactory;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/v1/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String apiKey = request.getHeader(API_KEY_HEADER);
		if (!StringUtils.hasText(apiKey)) {
			this.apiErrorResponseFactory.write(
				response,
				HttpStatus.UNAUTHORIZED,
				"MISSING_API_KEY",
				"X-API-Key header is required",
				List.of(new ApiErrorDetailResponse(API_KEY_HEADER, "X-API-Key header is required"))
			);
			return;
		}
		if (!this.configuredApiKey.equals(apiKey)) {
			this.apiErrorResponseFactory.write(
				response,
				HttpStatus.UNAUTHORIZED,
				"INVALID_API_KEY",
				"X-API-Key is invalid",
				List.of(new ApiErrorDetailResponse(API_KEY_HEADER, "X-API-Key is invalid"))
			);
			return;
		}

		UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
			"api-key-client",
			apiKey,
			AuthorityUtils.NO_AUTHORITIES
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}
}

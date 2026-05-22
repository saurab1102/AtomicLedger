package com.saurab.atomicledger.wallet.api;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiErrorResponseFactory {

	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	public ApiErrorResponse create(String errorCode, String message, List<ApiErrorDetailResponse> details) {
		return new ApiErrorResponse(errorCode, message, details, Instant.now());
	}

	public void write(
		HttpServletResponse response,
		HttpStatus status,
		String errorCode,
		String message,
		List<ApiErrorDetailResponse> details
	) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		this.objectMapper.writeValue(response.getOutputStream(), create(errorCode, message, details));
	}
}

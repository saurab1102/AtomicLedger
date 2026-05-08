package com.saurab.atomicledger.wallet.api;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.saurab.atomicledger.wallet.UnsupportedWalletCurrencyException;

@RestControllerAdvice
public class WalletExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<FieldErrorResponse> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.sorted(Comparator.comparing(FieldError::getField))
			.map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();
		return ResponseEntity.badRequest().body(new ValidationErrorResponse("Validation failed", errors));
	}

	@ExceptionHandler(UnsupportedWalletCurrencyException.class)
	public ResponseEntity<ValidationErrorResponse> handleUnsupportedWalletCurrency(UnsupportedWalletCurrencyException exception) {
		ValidationErrorResponse response = new ValidationErrorResponse(
			"Validation failed",
			List.of(new FieldErrorResponse("currency", exception.getMessage()))
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
}

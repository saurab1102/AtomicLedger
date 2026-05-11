package com.saurab.atomicledger.wallet.api;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.saurab.atomicledger.wallet.MissingIdempotencyKeyException;
import com.saurab.atomicledger.wallet.UnsupportedWalletCurrencyException;
import com.saurab.atomicledger.wallet.WalletNotActiveException;
import com.saurab.atomicledger.wallet.WalletNotFoundException;

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

	@ExceptionHandler(MissingIdempotencyKeyException.class)
	public ResponseEntity<ValidationErrorResponse> handleMissingIdempotencyKey(MissingIdempotencyKeyException exception) {
		ValidationErrorResponse response = new ValidationErrorResponse(
			"Validation failed",
			List.of(new FieldErrorResponse("Idempotency-Key", exception.getMessage()))
		);
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(WalletNotFoundException.class)
	public ResponseEntity<ValidationErrorResponse> handleWalletNotFound(WalletNotFoundException exception) {
		ValidationErrorResponse response = new ValidationErrorResponse(
			"Validation failed",
			List.of(new FieldErrorResponse("walletId", exception.getMessage()))
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(WalletNotActiveException.class)
	public ResponseEntity<ValidationErrorResponse> handleWalletNotActive(WalletNotActiveException exception) {
		ValidationErrorResponse response = new ValidationErrorResponse(
			"Validation failed",
			List.of(new FieldErrorResponse("walletId", exception.getMessage()))
		);
		return ResponseEntity.badRequest().body(response);
	}
}

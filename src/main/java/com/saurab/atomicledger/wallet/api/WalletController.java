package com.saurab.atomicledger.wallet.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

	private final WalletService walletService;

	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
		return this.walletService.createWallet(request);
	}

	@PostMapping("/{walletId}/deposit")
	@ResponseStatus(HttpStatus.CREATED)
	public DepositResponse deposit(
		@PathVariable UUID walletId,
		@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
		@Valid @RequestBody DepositWalletRequest request
	) {
		return this.walletService.deposit(walletId, idempotencyKey, request);
	}

	@GetMapping("/{walletId}/transactions")
	public WalletTransactionHistoryPageResponse transactionHistory(
		@PathVariable UUID walletId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "createdAt,desc") String sort
	) {
		return this.walletService.getTransactionHistory(walletId, page, size, sort);
	}
}

package com.saurab.atomicledger.wallet.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

	private final WalletService walletService;

	public TransferController(WalletService walletService) {
		this.walletService = walletService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransferResponse createTransfer(
		@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
		@Valid @RequestBody CreateTransferRequest request
	) {
		return this.walletService.transfer(idempotencyKey, request);
	}
}

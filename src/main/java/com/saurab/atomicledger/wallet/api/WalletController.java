package com.saurab.atomicledger.wallet.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}

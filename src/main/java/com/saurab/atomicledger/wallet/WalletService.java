package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saurab.atomicledger.wallet.api.CreateWalletRequest;
import com.saurab.atomicledger.wallet.api.WalletResponse;

@Service
public class WalletService {

	private static final BigDecimal INITIAL_AVAILABLE_BALANCE = BigDecimal.ZERO.setScale(2);

	private final WalletRepository walletRepository;

	public WalletService(WalletRepository walletRepository) {
		this.walletRepository = walletRepository;
	}

	@Transactional
	public WalletResponse createWallet(CreateWalletRequest request) {
		WalletCurrency currency = parseCurrency(request.currency());

		Wallet wallet = new Wallet(
			UUID.randomUUID(),
			request.ownerReference().trim(),
			currency,
			INITIAL_AVAILABLE_BALANCE,
			WalletStatus.ACTIVE
		);

		Wallet savedWallet = this.walletRepository.save(wallet);

		return new WalletResponse(
			savedWallet.getId(),
			savedWallet.getOwnerReference(),
			savedWallet.getCurrency().name(),
			savedWallet.getAvailableBalance(),
			savedWallet.getStatus().name()
		);
	}

	private WalletCurrency parseCurrency(String currency) {
		String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

		if (!WalletCurrency.INR.name().equals(normalizedCurrency)) {
			throw new UnsupportedWalletCurrencyException(currency);
		}
		return WalletCurrency.INR;
	}
}

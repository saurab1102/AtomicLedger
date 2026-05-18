package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.saurab.atomicledger.wallet.api.CreateWalletRequest;
import com.saurab.atomicledger.wallet.api.CreateTransferRequest;
import com.saurab.atomicledger.wallet.api.DepositResponse;
import com.saurab.atomicledger.wallet.api.DepositWalletRequest;
import com.saurab.atomicledger.wallet.api.TransferResponse;
import com.saurab.atomicledger.wallet.api.WalletResponse;

@Service
public class WalletService {

	private static final BigDecimal INITIAL_AVAILABLE_BALANCE = BigDecimal.ZERO.setScale(2);

	private final WalletRepository walletRepository;
	private final WalletTransactionRepository walletTransactionRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final TransactionTemplate transactionTemplate;

	public WalletService(
		WalletRepository walletRepository,
		WalletTransactionRepository walletTransactionRepository,
		LedgerEntryRepository ledgerEntryRepository,
		PlatformTransactionManager transactionManager
	) {
		this.walletRepository = walletRepository;
		this.walletTransactionRepository = walletTransactionRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Transactional
	public WalletResponse createWallet(CreateWalletRequest request) {
		WalletCurrency currency = WalletCurrency.from(request.currency());

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

	public DepositResponse deposit(UUID walletId, String idempotencyKey, DepositWalletRequest request) {
		String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey);

		if (existingTransaction.isPresent()) {
			return toDepositResponse(existingTransaction.get());
		}

		WalletCurrency currency = WalletCurrency.from(request.currency());
		BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

		try {
			return this.transactionTemplate.execute(status -> createDeposit(walletId, normalizedIdempotencyKey, amount, currency));
		}
		catch (DataIntegrityViolationException exception) {
			return this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey)
				.map(this::toDepositResponse)
				.orElseThrow(() -> exception);
		}
	}

	private DepositResponse createDeposit(UUID walletId, String idempotencyKey, BigDecimal amount, WalletCurrency currency) {
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(idempotencyKey);

		if (existingTransaction.isPresent()) {
			return toDepositResponse(existingTransaction.get());
		}

		Wallet wallet = this.walletRepository.findByIdForUpdate(walletId)
			.orElseThrow(() -> new WalletNotFoundException(walletId));

		if (!wallet.isActive()) {
			throw new WalletNotActiveException(walletId);
		}

		BigDecimal resultingAvailableBalance = wallet.getAvailableBalance().add(amount);

		WalletTransaction transaction = this.walletTransactionRepository.saveAndFlush(new WalletTransaction(
			UUID.randomUUID(),
			wallet,
			null,
			idempotencyKey,
			WalletTransactionType.DEPOSIT,
			WalletTransactionStatus.SUCCEEDED,
			amount,
			currency,
			resultingAvailableBalance,
			null
		));

		this.ledgerEntryRepository.save(new LedgerEntry(
			UUID.randomUUID(),
			transaction,
			wallet,
			LedgerEntryType.CREDIT,
			amount,
			currency
		));

		wallet.credit(amount);

		return toDepositResponse(transaction);
	}

	public TransferResponse transfer(String idempotencyKey, CreateTransferRequest request) {
		String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey);

		if (existingTransaction.isPresent()) {
			return toTransferResponse(existingTransaction.get());
		}

		WalletCurrency currency = WalletCurrency.from(request.currency());
		BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

		try {
			return this.transactionTemplate.execute(status -> createTransfer(normalizedIdempotencyKey, request, amount, currency));
		}
		catch (DataIntegrityViolationException exception) {
			return this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey)
				.map(this::toTransferResponse)
				.orElseThrow(() -> exception);
		}
	}

	private TransferResponse createTransfer(
		String idempotencyKey,
		CreateTransferRequest request,
		BigDecimal amount,
		WalletCurrency currency
	) {
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(idempotencyKey);

		if (existingTransaction.isPresent()) {
			return toTransferResponse(existingTransaction.get());
		}

		if (request.sourceWalletId().equals(request.destinationWalletId())) {
			throw new SameWalletTransferException();
		}

		Map<UUID, Wallet> walletsById = loadWalletsForTransfer(request.sourceWalletId(), request.destinationWalletId()).stream()
			.collect(Collectors.toMap(Wallet::getId, wallet -> wallet));

		Wallet sourceWallet = Optional.ofNullable(walletsById.get(request.sourceWalletId()))
			.orElseThrow(() -> new WalletNotFoundException("sourceWalletId", request.sourceWalletId()));
		Wallet destinationWallet = Optional.ofNullable(walletsById.get(request.destinationWalletId()))
			.orElseThrow(() -> new WalletNotFoundException("destinationWalletId", request.destinationWalletId()));

		if (!sourceWallet.isActive()) {
			throw new WalletNotActiveException("sourceWalletId", sourceWallet.getId());
		}
		if (!destinationWallet.isActive()) {
			throw new WalletNotActiveException("destinationWalletId", destinationWallet.getId());
		}
		if (sourceWallet.getCurrency() != currency || destinationWallet.getCurrency() != currency) {
			throw new WalletCurrencyMismatchException();
		}
		if (sourceWallet.getAvailableBalance().compareTo(amount) < 0) {
			throw new InsufficientAvailableBalanceException();
		}

		BigDecimal sourceAvailableBalance = sourceWallet.getAvailableBalance().subtract(amount);
		BigDecimal destinationAvailableBalance = destinationWallet.getAvailableBalance().add(amount);

		WalletTransaction transaction = this.walletTransactionRepository.saveAndFlush(new WalletTransaction(
			UUID.randomUUID(),
			sourceWallet,
			destinationWallet,
			idempotencyKey,
			WalletTransactionType.TRANSFER,
			WalletTransactionStatus.SUCCEEDED,
			amount,
			currency,
			sourceAvailableBalance,
			destinationAvailableBalance
		));

		this.ledgerEntryRepository.save(new LedgerEntry(
			UUID.randomUUID(),
			transaction,
			sourceWallet,
			LedgerEntryType.DEBIT,
			amount,
			currency
		));
		this.ledgerEntryRepository.save(new LedgerEntry(
			UUID.randomUUID(),
			transaction,
			destinationWallet,
			LedgerEntryType.CREDIT,
			amount,
			currency
		));

		sourceWallet.debit(amount);
		destinationWallet.credit(amount);

		return toTransferResponse(transaction);
	}

	private List<Wallet> loadWalletsForTransfer(UUID sourceWalletId, UUID destinationWalletId) {
		List<UUID> walletIdsInLockOrder = java.util.stream.Stream.of(sourceWalletId, destinationWalletId)
			.sorted()
			.toList();

		return this.walletRepository.findAllByIdInOrderByIdForUpdate(walletIdsInLockOrder);
	}

	private DepositResponse toDepositResponse(WalletTransaction transaction) {
		return new DepositResponse(
			transaction.getId(),
			transaction.getWallet().getId(),
			transaction.getAmount(),
			transaction.getCurrency().name(),
			transaction.getTransactionType().name(),
			transaction.getStatus().name(),
			transaction.getResultingAvailableBalance()
		);
	}

	private TransferResponse toTransferResponse(WalletTransaction transaction) {
		if (transaction.getCounterpartyWallet() == null || transaction.getCounterpartyResultingAvailableBalance() == null) {
			throw new IllegalStateException("idempotency key is already used for a different operation");
		}
		return new TransferResponse(
			transaction.getId(),
			transaction.getWallet().getId(),
			transaction.getCounterpartyWallet().getId(),
			transaction.getAmount(),
			transaction.getCurrency().name(),
			transaction.getTransactionType().name(),
			transaction.getStatus().name(),
			transaction.getResultingAvailableBalance(),
			transaction.getCounterpartyResultingAvailableBalance()
		);
	}

	private String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new MissingIdempotencyKeyException();
		}
		return idempotencyKey.trim();
	}
}

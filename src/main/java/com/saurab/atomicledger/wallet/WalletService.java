package com.saurab.atomicledger.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import io.micrometer.core.instrument.Timer;

import com.saurab.atomicledger.wallet.api.CreateWalletRequest;
import com.saurab.atomicledger.wallet.api.CreateTransferRequest;
import com.saurab.atomicledger.wallet.api.DepositResponse;
import com.saurab.atomicledger.wallet.api.DepositWalletRequest;
import com.saurab.atomicledger.wallet.api.TransferResponse;
import com.saurab.atomicledger.wallet.api.WalletTransactionHistoryItemResponse;
import com.saurab.atomicledger.wallet.api.WalletTransactionHistoryPageResponse;
import com.saurab.atomicledger.wallet.api.WalletResponse;

@Service
public class WalletService {

	private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);
	private static final BigDecimal INITIAL_AVAILABLE_BALANCE = BigDecimal.ZERO.setScale(2);

	private final WalletRepository walletRepository;
	private final WalletTransactionRepository walletTransactionRepository;
	private final LedgerEntryRepository ledgerEntryRepository;
	private final AuditLogService auditLogService;
	private final OutboxEventService outboxEventService;
	private final OperationalMetrics operationalMetrics;
	private final TransactionTemplate transactionTemplate;

	public WalletService(
		WalletRepository walletRepository,
		WalletTransactionRepository walletTransactionRepository,
		LedgerEntryRepository ledgerEntryRepository,
		AuditLogService auditLogService,
		OutboxEventService outboxEventService,
		OperationalMetrics operationalMetrics,
		PlatformTransactionManager transactionManager
	) {
		this.walletRepository = walletRepository;
		this.walletTransactionRepository = walletTransactionRepository;
		this.ledgerEntryRepository = ledgerEntryRepository;
		this.auditLogService = auditLogService;
		this.outboxEventService = outboxEventService;
		this.operationalMetrics = operationalMetrics;
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
		this.auditLogService.recordInCurrentTransaction(
			AuditAction.WALLET_CREATED,
			AuditEntityType.WALLET,
			savedWallet.getId().toString(),
			Map.of("ownerReference", savedWallet.getOwnerReference(), "currency", savedWallet.getCurrency().name())
		);
		this.outboxEventService.recordInCurrentTransaction(
			OutboxEventType.WALLET_CREATED,
			OutboxAggregateType.WALLET,
			savedWallet.getId().toString(),
			Map.of("ownerReference", savedWallet.getOwnerReference(), "currency", savedWallet.getCurrency().name())
		);
		this.operationalMetrics.incrementWalletsCreated();

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
			// Duplicate replays do not create new state changes; they only surface the original result.
			recordDuplicateDepositAudit(existingTransaction.get());
			return toDepositResponse(existingTransaction.get());
		}

		WalletCurrency currency = WalletCurrency.from(request.currency());
		BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

		try {
			return this.transactionTemplate.execute(status -> createDeposit(walletId, normalizedIdempotencyKey, amount, currency));
		}
		catch (DataIntegrityViolationException exception) {
			return this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey)
				.map(transaction -> {
					recordDuplicateDepositAudit(transaction);
					return toDepositResponse(transaction);
				})
				.orElseThrow(() -> exception);
		}
	}

	private DepositResponse createDeposit(UUID walletId, String idempotencyKey, BigDecimal amount, WalletCurrency currency) {
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(idempotencyKey);

		if (existingTransaction.isPresent()) {
			recordDuplicateDepositAudit(existingTransaction.get());
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
			null,
			Instant.now()
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
		this.auditLogService.recordInCurrentTransaction(
			AuditAction.DEPOSIT_SUCCEEDED,
			AuditEntityType.TRANSACTION,
			transaction.getId().toString(),
			Map.of(
				"walletId", wallet.getId().toString(),
				"amount", amount,
				"currency", currency.name()
			)
		);
		this.outboxEventService.recordInCurrentTransaction(
			OutboxEventType.DEPOSIT_SUCCEEDED,
			OutboxAggregateType.TRANSACTION,
			transaction.getId().toString(),
			Map.of(
				"walletId", wallet.getId().toString(),
				"amount", amount,
				"currency", currency.name()
			)
		);
		this.operationalMetrics.incrementDepositsSucceeded();
		logDepositOutcome("deposit_succeeded", wallet.getId(), transaction.getId(), idempotencyKey);

		return toDepositResponse(transaction);
	}

	public TransferResponse transfer(String idempotencyKey, CreateTransferRequest request) {
		String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
		Timer.Sample transferSample = this.operationalMetrics.startTransferProcessingSample();
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey);

		try {
			if (existingTransaction.isPresent()) {
				// A reused idempotency key must return the first committed transfer response.
				recordDuplicateTransferAudit(existingTransaction.get());
				return toTransferResponse(existingTransaction.get());
			}

			WalletCurrency currency = WalletCurrency.from(request.currency());
			BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

			TransferAttemptResult transferAttemptResult = this.transactionTemplate.execute(
				status -> createTransfer(normalizedIdempotencyKey, request, amount, currency)
			);
			if (transferAttemptResult == null) {
				throw new IllegalStateException("transfer transaction returned no result");
			}
			if (transferAttemptResult.failed()) {
				// The failure audit/outbox records were already committed inside the transaction.
				this.operationalMetrics.incrementTransfersFailed();
				logTransferFailed(normalizedIdempotencyKey, request.sourceWalletId(), request.destinationWalletId());
				throw new InsufficientAvailableBalanceException();
			}
			if (transferAttemptResult.replayed()) {
				recordDuplicateTransferAudit(this.walletTransactionRepository.findById(transferAttemptResult.response().transactionId()).orElseThrow());
				return transferAttemptResult.response();
			}
			this.operationalMetrics.incrementTransfersSucceeded();
			logTransferSucceeded(transferAttemptResult.response(), normalizedIdempotencyKey);
			return transferAttemptResult.response();
		}
		catch (DataIntegrityViolationException exception) {
			return this.walletTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey)
				.map(transaction -> {
					recordDuplicateTransferAudit(transaction);
					return toTransferResponse(transaction);
				})
				.orElseThrow(() -> exception);
		}
		finally {
			this.operationalMetrics.recordTransferProcessingDuration(transferSample);
		}
	}

	private TransferAttemptResult createTransfer(
		String idempotencyKey,
		CreateTransferRequest request,
		BigDecimal amount,
		WalletCurrency currency
	) {
		Optional<WalletTransaction> existingTransaction = this.walletTransactionRepository.findByIdempotencyKey(idempotencyKey);

		if (existingTransaction.isPresent()) {
			return TransferAttemptResult.replayed(toTransferResponse(existingTransaction.get()));
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
			// We persist the failed business outcome before surfacing the validation error so the
			// outbox event is committed rather than rolled back with the exception.
			recordInsufficientTransferAudit(sourceWallet.getId(), destinationWallet.getId(), amount, currency);
			this.outboxEventService.recordInCurrentTransaction(
				OutboxEventType.TRANSFER_INSUFFICIENT_BALANCE,
				OutboxAggregateType.WALLET,
				sourceWallet.getId().toString(),
				Map.of(
					"sourceWalletId", sourceWallet.getId().toString(),
					"destinationWalletId", destinationWallet.getId().toString(),
					"amount", amount,
					"currency", currency.name()
				)
			);
			return TransferAttemptResult.failure();
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
			destinationAvailableBalance,
			Instant.now()
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
		this.auditLogService.recordInCurrentTransaction(
			AuditAction.TRANSFER_SUCCEEDED,
			AuditEntityType.TRANSACTION,
			transaction.getId().toString(),
			Map.of(
				"sourceWalletId", sourceWallet.getId().toString(),
				"destinationWalletId", destinationWallet.getId().toString(),
				"amount", amount,
				"currency", currency.name()
			)
		);
		this.outboxEventService.recordInCurrentTransaction(
			OutboxEventType.TRANSFER_SUCCEEDED,
			OutboxAggregateType.TRANSACTION,
			transaction.getId().toString(),
			Map.of(
				"sourceWalletId", sourceWallet.getId().toString(),
				"destinationWalletId", destinationWallet.getId().toString(),
				"amount", amount,
				"currency", currency.name()
			)
		);

		return TransferAttemptResult.success(toTransferResponse(transaction));
	}

	private List<Wallet> loadWalletsForTransfer(UUID sourceWalletId, UUID destinationWalletId) {
		// Lock wallets in a stable order to reduce deadlock risk when transfers race each other.
		List<UUID> walletIdsInLockOrder = java.util.stream.Stream.of(sourceWalletId, destinationWalletId)
			.sorted()
			.toList();

		return this.walletRepository.findAllByIdInOrderByIdForUpdate(walletIdsInLockOrder);
	}

	@Transactional(readOnly = true)
	public WalletTransactionHistoryPageResponse getTransactionHistory(UUID walletId, int page, int size, String sort) {
		if (!this.walletRepository.existsById(walletId)) {
			throw new WalletNotFoundException(walletId);
		}

		PageRequest pageRequest = PageRequest.of(page, size, toHistorySort(sort));
		Page<LedgerEntry> historyPage = this.ledgerEntryRepository.findAllByWalletId(walletId, pageRequest);

		return new WalletTransactionHistoryPageResponse(
			historyPage.getContent().stream().map(this::toHistoryItem).toList(),
			historyPage.getNumber(),
			historyPage.getSize(),
			historyPage.getTotalElements(),
			historyPage.getTotalPages()
		);
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

	private WalletTransactionHistoryItemResponse toHistoryItem(LedgerEntry ledgerEntry) {
		WalletTransaction transaction = ledgerEntry.getTransaction();
		UUID counterpartyWalletId = transaction.getTransactionType() == WalletTransactionType.DEPOSIT
			? null
			: ledgerEntry.getWallet().getId().equals(transaction.getWallet().getId())
				? transaction.getCounterpartyWallet().getId()
				: transaction.getWallet().getId();

		return new WalletTransactionHistoryItemResponse(
			transaction.getId(),
			transaction.getTransactionType().name(),
			transaction.getStatus().name(),
			ledgerEntry.getEntryType().name(),
			transaction.getAmount(),
			transaction.getCurrency().name(),
			counterpartyWalletId,
			transaction.getCreatedAt()
		);
	}

	private Sort toHistorySort(String sort) {
		String normalizedSort = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();
		String[] parts = normalizedSort.split(",", 2);
		String field = parts[0].trim();
		String direction = parts.length > 1 ? parts[1].trim() : "desc";

		String property = switch (field) {
			case "amount" -> "transaction.amount";
			case "currency" -> "transaction.currency";
			case "status" -> "transaction.status";
			case "type" -> "transaction.transactionType";
			case "createdAt" -> "transaction.createdAt";
			default -> "transaction.createdAt";
		};

		Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return Sort.by(new Sort.Order(sortDirection, property), new Sort.Order(Sort.Direction.DESC, "transaction.id"));
	}

	private String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new MissingIdempotencyKeyException();
		}
		return idempotencyKey.trim();
	}

	private void recordDuplicateDepositAudit(WalletTransaction transaction) {
		this.auditLogService.recordStandalone(
			AuditAction.DEPOSIT_DUPLICATE_REPLAY,
			AuditEntityType.WALLET,
			transaction.getWallet().getId().toString(),
			Map.of(
				"transactionId", transaction.getId().toString(),
				"idempotencyKey", transaction.getIdempotencyKey()
			)
		);
		this.operationalMetrics.incrementDepositDuplicateReplays();
		logDepositOutcome(
			"deposit_duplicate_replay",
			transaction.getWallet().getId(),
			transaction.getId(),
			transaction.getIdempotencyKey()
		);
	}

	private void recordDuplicateTransferAudit(WalletTransaction transaction) {
		this.auditLogService.recordStandalone(
			AuditAction.TRANSFER_DUPLICATE_REPLAY,
			AuditEntityType.TRANSACTION,
			transaction.getId().toString(),
			Map.of(
				"sourceWalletId", transaction.getWallet().getId().toString(),
				"destinationWalletId", transaction.getCounterpartyWallet().getId().toString(),
				"idempotencyKey", transaction.getIdempotencyKey()
			)
		);
		this.operationalMetrics.incrementTransferDuplicateReplays();
		logTransferReplay(transaction, transaction.getIdempotencyKey());
	}

	private void recordInsufficientTransferAudit(
		UUID sourceWalletId,
		UUID destinationWalletId,
		BigDecimal amount,
		WalletCurrency currency
	) {
		this.auditLogService.recordInCurrentTransaction(
			AuditAction.TRANSFER_INSUFFICIENT_BALANCE,
			AuditEntityType.WALLET,
			sourceWalletId.toString(),
			Map.of(
				"sourceWalletId", sourceWalletId.toString(),
				"destinationWalletId", destinationWalletId.toString(),
				"amount", amount,
				"currency", currency.name()
			)
		);
	}

	private void logDepositOutcome(String eventName, UUID walletId, UUID transactionId, String idempotencyKey) {
		LOGGER.atInfo()
			.addKeyValue("walletId", walletId)
			.addKeyValue("transactionId", transactionId)
			.addKeyValue("idempotencyKey", idempotencyKey)
			.log(eventName);
	}

	private void logTransferSucceeded(TransferResponse response, String idempotencyKey) {
		LOGGER.atInfo()
			.addKeyValue("transactionId", response.transactionId())
			.addKeyValue("sourceWalletId", response.sourceWalletId())
			.addKeyValue("destinationWalletId", response.destinationWalletId())
			.addKeyValue("idempotencyKey", idempotencyKey)
			.log("transfer_succeeded");
	}

	private void logTransferReplay(WalletTransaction transaction, String idempotencyKey) {
		LOGGER.atInfo()
			.addKeyValue("transactionId", transaction.getId())
			.addKeyValue("sourceWalletId", transaction.getWallet().getId())
			.addKeyValue("destinationWalletId", transaction.getCounterpartyWallet().getId())
			.addKeyValue("idempotencyKey", idempotencyKey)
			.log("transfer_duplicate_replay");
	}

	private void logTransferFailed(String idempotencyKey, UUID sourceWalletId, UUID destinationWalletId) {
		LOGGER.atInfo()
			.addKeyValue("sourceWalletId", sourceWalletId)
			.addKeyValue("destinationWalletId", destinationWalletId)
			.addKeyValue("idempotencyKey", idempotencyKey)
			.log("transfer_failed");
	}

	private record TransferAttemptResult(TransferResponse response, TransferAttemptOutcome outcome) {

		private static TransferAttemptResult success(TransferResponse response) {
			return new TransferAttemptResult(response, TransferAttemptOutcome.SUCCEEDED);
		}

		private static TransferAttemptResult failure() {
			return new TransferAttemptResult(null, TransferAttemptOutcome.FAILED);
		}

		private static TransferAttemptResult replayed(TransferResponse response) {
			return new TransferAttemptResult(response, TransferAttemptOutcome.DUPLICATE_REPLAY);
		}

		private boolean failed() {
			return this.outcome == TransferAttemptOutcome.FAILED;
		}

		private boolean replayed() {
			return this.outcome == TransferAttemptOutcome.DUPLICATE_REPLAY;
		}
	}

	private enum TransferAttemptOutcome {
		SUCCEEDED,
		FAILED,
		DUPLICATE_REPLAY
	}
}

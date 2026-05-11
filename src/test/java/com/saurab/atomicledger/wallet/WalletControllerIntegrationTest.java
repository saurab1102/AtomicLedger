package com.saurab.atomicledger.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.saurab.atomicledger.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private WalletTransactionRepository walletTransactionRepository;

	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;

	@BeforeEach
	void setUp() {
		this.ledgerEntryRepository.deleteAll();
		this.walletTransactionRepository.deleteAll();
		this.walletRepository.deleteAll();
	}

	@Test
	void createsWalletSuccessfullyAndPersistsIt() throws Exception {
		MvcResult result = this.mockMvc.perform(post("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "customer-123",
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.ownerReference").value("customer-123"))
			.andExpect(jsonPath("$.currency").value("INR"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andReturn();

		UUID walletId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));

		Wallet persistedWallet = this.walletRepository.findById(walletId).orElseThrow();
		assertThat(persistedWallet.getOwnerReference()).isEqualTo("customer-123");
		assertThat(persistedWallet.getCurrency()).isEqualTo(WalletCurrency.INR);
		assertThat(persistedWallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
		assertThat(persistedWallet.getAvailableBalance()).isEqualByComparingTo("0.00");
	}

	@Test
	void rejectsMissingOwnerReference() throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("ownerReference"))
			.andExpect(jsonPath("$.errors[0].message").value("ownerReference is required"));
	}

	@Test
	void rejectsUnsupportedCurrency() throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "customer-123",
				  "currency": "USD"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("currency"))
			.andExpect(jsonPath("$.errors[0].message").value("currency is unsupported"));
	}

	@Test
	void initializesWalletWithZeroAvailableBalanceAndActiveStatus() throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "customer-456",
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.availableBalance").value(0.0))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		Wallet persistedWallet = this.walletRepository.findAll().stream().findFirst().orElseThrow();
		assertThat(persistedWallet.getAvailableBalance()).isEqualByComparingTo("0.00");
		assertThat(persistedWallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
	}

	@Test
	void depositsSuccessfullyAndPersistsAccountingRecords() throws Exception {
		UUID walletId = createWallet("depositor-123");

		MvcResult result = this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-001")
			.content("""
				{
				  "amount": 125.50,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.walletId").value(walletId.toString()))
			.andExpect(jsonPath("$.amount").value(125.5))
			.andExpect(jsonPath("$.currency").value("INR"))
			.andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
			.andExpect(jsonPath("$.transactionStatus").value("SUCCEEDED"))
			.andExpect(jsonPath("$.availableBalance").value(125.5))
			.andReturn();

		UUID transactionId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.transactionId"));

		Wallet persistedWallet = this.walletRepository.findById(walletId).orElseThrow();
		assertThat(persistedWallet.getAvailableBalance()).isEqualByComparingTo("125.50");

		WalletTransaction transaction = this.walletTransactionRepository.findById(transactionId).orElseThrow();
		assertThat(transaction.getTransactionType()).isEqualTo(WalletTransactionType.DEPOSIT);
		assertThat(transaction.getStatus()).isEqualTo(WalletTransactionStatus.SUCCEEDED);
		assertThat(transaction.getAmount()).isEqualByComparingTo("125.50");
		assertThat(transaction.getCurrency()).isEqualTo(WalletCurrency.INR);
		assertThat(transaction.getResultingAvailableBalance()).isEqualByComparingTo("125.50");

		LedgerEntry ledgerEntry = this.ledgerEntryRepository.findAll().stream().findFirst().orElseThrow();
		assertThat(ledgerEntry.getTransaction().getId()).isEqualTo(transactionId);
		assertThat(ledgerEntry.getWallet().getId()).isEqualTo(walletId);
		assertThat(ledgerEntry.getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
		assertThat(ledgerEntry.getAmount()).isEqualByComparingTo("125.50");
		assertThat(ledgerEntry.getCurrency()).isEqualTo(WalletCurrency.INR);
		assertThat(this.walletTransactionRepository.findAll()).hasSize(1);
		assertThat(this.ledgerEntryRepository.findAll()).hasSize(1);
	}

	@Test
	void rejectsMissingIdempotencyKey() throws Exception {
		UUID walletId = createWallet("depositor-124");

		this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "amount": 10.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"))
			.andExpect(jsonPath("$.errors[0].message").value("Idempotency-Key header is required"));
	}

	@Test
	void rejectsInvalidAmount() throws Exception {
		UUID walletId = createWallet("depositor-125");

		this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-002")
			.content("""
				{
				  "amount": 0,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("amount"))
			.andExpect(jsonPath("$.errors[0].message").value("amount must be positive"));
	}

	@Test
	void rejectsUnsupportedDepositCurrency() throws Exception {
		UUID walletId = createWallet("depositor-126");

		this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-003")
			.content("""
				{
				  "amount": 10.00,
				  "currency": "USD"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("currency"))
			.andExpect(jsonPath("$.errors[0].message").value("currency is unsupported"));
	}

	@Test
	void rejectsDepositWhenWalletIsMissing() throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", UUID.randomUUID())
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-004")
			.content("""
				{
				  "amount": 10.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("walletId"))
			.andExpect(jsonPath("$.errors[0].message").value("wallet not found"));
	}

	@Test
	void reusesOriginalResultForDuplicateIdempotencyKey() throws Exception {
		UUID walletId = createWallet("depositor-127");

		MvcResult firstResult = this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-duplicate")
			.content("""
				{
				  "amount": 75.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated())
			.andReturn();

		String firstBody = firstResult.getResponse().getContentAsString();

		MvcResult secondResult = this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-duplicate")
			.content("""
				{
				  "amount": 75.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
			.andExpect(jsonPath("$.transactionStatus").value("SUCCEEDED"))
			.andExpect(jsonPath("$.availableBalance").value(75.0))
			.andReturn();

		assertThat(secondResult.getResponse().getContentAsString()).isEqualTo(firstBody);
		assertThat(this.walletTransactionRepository.findAll()).hasSize(1);
		assertThat(this.ledgerEntryRepository.findAll()).hasSize(1);
		assertThat(this.walletRepository.findById(walletId).orElseThrow().getAvailableBalance()).isEqualByComparingTo("75.00");
	}

	@Test
	void transfersSuccessfullyAndPersistsAccountingRecords() throws Exception {
		UUID sourceWalletId = createWallet("source-001");
		UUID destinationWalletId = createWallet("destination-001");
		deposit(sourceWalletId, "seed-source-001", "200.00");

		MvcResult result = this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-001")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 75.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.sourceWalletId").value(sourceWalletId.toString()))
			.andExpect(jsonPath("$.destinationWalletId").value(destinationWalletId.toString()))
			.andExpect(jsonPath("$.amount").value(75.0))
			.andExpect(jsonPath("$.currency").value("INR"))
			.andExpect(jsonPath("$.transactionType").value("TRANSFER"))
			.andExpect(jsonPath("$.transactionStatus").value("SUCCEEDED"))
			.andExpect(jsonPath("$.sourceAvailableBalance").value(125.0))
			.andExpect(jsonPath("$.destinationAvailableBalance").value(75.0))
			.andReturn();

		UUID transactionId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.transactionId"));

		Wallet sourceWallet = this.walletRepository.findById(sourceWalletId).orElseThrow();
		Wallet destinationWallet = this.walletRepository.findById(destinationWalletId).orElseThrow();
		assertThat(sourceWallet.getAvailableBalance()).isEqualByComparingTo("125.00");
		assertThat(destinationWallet.getAvailableBalance()).isEqualByComparingTo("75.00");

		WalletTransaction transaction = this.walletTransactionRepository.findById(transactionId).orElseThrow();
		assertThat(transaction.getTransactionType()).isEqualTo(WalletTransactionType.TRANSFER);
		assertThat(transaction.getStatus()).isEqualTo(WalletTransactionStatus.SUCCEEDED);
		assertThat(transaction.getWallet().getId()).isEqualTo(sourceWalletId);
		assertThat(transaction.getCounterpartyWallet().getId()).isEqualTo(destinationWalletId);
		assertThat(transaction.getResultingAvailableBalance()).isEqualByComparingTo("125.00");
		assertThat(transaction.getCounterpartyResultingAvailableBalance()).isEqualByComparingTo("75.00");

		List<LedgerEntry> entries = this.ledgerEntryRepository.findAllByTransactionId(transactionId).stream()
			.sorted(Comparator.comparing(entry -> entry.getWallet().getId()))
			.toList();
		assertThat(entries).hasSize(2);
		assertThat(entries.stream().filter(entry -> entry.getWallet().getId().equals(sourceWalletId)).findFirst().orElseThrow().getEntryType())
			.isEqualTo(LedgerEntryType.DEBIT);
		assertThat(entries.stream().filter(entry -> entry.getWallet().getId().equals(destinationWalletId)).findFirst().orElseThrow().getEntryType())
			.isEqualTo(LedgerEntryType.CREDIT);
	}

	@Test
	void rejectsTransferWhenSourceBalanceIsInsufficient() throws Exception {
		UUID sourceWalletId = createWallet("source-002");
		UUID destinationWalletId = createWallet("destination-002");

		this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-002")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("amount"))
			.andExpect(jsonPath("$.errors[0].message").value("insufficient available balance"));
	}

	@Test
	void rejectsTransferWhenSourceAndDestinationAreTheSame() throws Exception {
		UUID walletId = createWallet("same-wallet");
		deposit(walletId, "seed-same-wallet", "50.00");

		this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-003")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "INR"
				}
				""".formatted(walletId, walletId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("destinationWalletId"))
			.andExpect(jsonPath("$.errors[0].message").value("source and destination wallets must be different"));
	}

	@Test
	void rejectsTransferWhenIdempotencyKeyIsMissing() throws Exception {
		UUID sourceWalletId = createWallet("source-003");
		UUID destinationWalletId = createWallet("destination-003");
		deposit(sourceWalletId, "seed-source-003", "30.00");

		this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"))
			.andExpect(jsonPath("$.errors[0].message").value("Idempotency-Key header is required"));
	}

	@Test
	void rejectsTransferWhenCurrencyIsUnsupported() throws Exception {
		UUID sourceWalletId = createWallet("source-004");
		UUID destinationWalletId = createWallet("destination-004");
		deposit(sourceWalletId, "seed-source-004", "30.00");

		this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-004")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "USD"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("currency"))
			.andExpect(jsonPath("$.errors[0].message").value("currency is unsupported"));
	}

	@Test
	void rejectsTransferWhenWalletIsMissing() throws Exception {
		UUID sourceWalletId = createWallet("source-005");
		deposit(sourceWalletId, "seed-source-005", "30.00");

		this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-005")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, UUID.randomUUID())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("destinationWalletId"))
			.andExpect(jsonPath("$.errors[0].message").value("wallet not found"));
	}

	@Test
	void reusesOriginalResultForDuplicateTransferIdempotencyKey() throws Exception {
		UUID sourceWalletId = createWallet("source-006");
		UUID destinationWalletId = createWallet("destination-006");
		deposit(sourceWalletId, "seed-source-006", "120.00");

		MvcResult firstResult = this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-duplicate")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 20.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isCreated())
			.andReturn();

		String firstBody = firstResult.getResponse().getContentAsString();
		UUID transactionId = UUID.fromString(JsonPath.read(firstBody, "$.transactionId"));

		MvcResult secondResult = this.mockMvc.perform(post("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "transfer-duplicate")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 20.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.transactionType").value("TRANSFER"))
			.andExpect(jsonPath("$.transactionStatus").value("SUCCEEDED"))
			.andReturn();

		assertThat(secondResult.getResponse().getContentAsString()).isEqualTo(firstBody);
		assertThat(this.walletTransactionRepository.findAll().stream()
			.filter(tx -> tx.getTransactionType() == WalletTransactionType.TRANSFER))
			.hasSize(1);
		assertThat(this.ledgerEntryRepository.findAllByTransactionId(transactionId)).hasSize(2);
		assertThat(this.walletRepository.findById(sourceWalletId).orElseThrow().getAvailableBalance()).isEqualByComparingTo("100.00");
		assertThat(this.walletRepository.findById(destinationWalletId).orElseThrow().getAvailableBalance()).isEqualByComparingTo("20.00");
	}

	private void deposit(UUID walletId, String idempotencyKey, String amount) throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", idempotencyKey)
			.content("""
				{
				  "amount": %s,
				  "currency": "INR"
				}
				""".formatted(amount)))
			.andExpect(status().isCreated());
	}

	private UUID createWallet(String ownerReference) throws Exception {
		MvcResult result = this.mockMvc.perform(post("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "%s",
				  "currency": "INR"
				}
				""".formatted(ownerReference)))
			.andExpect(status().isCreated())
			.andReturn();

		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}
}

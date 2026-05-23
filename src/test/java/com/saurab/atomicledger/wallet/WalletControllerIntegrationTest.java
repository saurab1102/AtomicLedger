package com.saurab.atomicledger.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.saurab.atomicledger.PostgresIntegrationTest;

@SpringBootTest(properties = {
	"atomicledger.scheduling.enabled=false",
	"atomicledger.security.api-key=test-api-key"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WalletControllerIntegrationTest extends PostgresIntegrationTest {

	private static final String TEST_API_KEY = "test-api-key";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private WalletTransactionRepository walletTransactionRepository;

	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxEventWorker outboxEventWorker;

	@BeforeEach
	void setUp() {
		this.outboxEventRepository.deleteAll();
		this.auditLogRepository.deleteAll();
		this.ledgerEntryRepository.deleteAll();
		this.walletTransactionRepository.deleteAll();
		this.walletRepository.deleteAll();
	}

	@Test
	void createsWalletSuccessfullyAndPersistsIt() throws Exception {
		MvcResult result = this.mockMvc.perform(apiPost("/api/v1/wallets")
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
	void exposesOpenApiDocumentation() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(responseBody, "$.info.title")).isEqualTo("AtomicLedger API");
		assertThat(JsonPath.<String>read(responseBody, "$.info.version")).isEqualTo("v1");
		assertThat(JsonPath.<String>read(responseBody, "$.paths['/api/v1/transfers'].post.summary")).isEqualTo("Create transfer");
		assertThat(JsonPath.<String>read(responseBody, "$.paths['/api/v1/wallets'].post.responses['400'].content['application/json'].schema.$ref"))
			.isEqualTo("#/components/schemas/ApiErrorResponse");
		assertThat(JsonPath.<String>read(responseBody, "$.components.schemas.ApiErrorResponse.properties.errorCode.type"))
			.isEqualTo("string");
		assertThat(JsonPath.<String>read(responseBody, "$.components.schemas.ApiErrorResponse.properties.details.type"))
			.isEqualTo("array");
		assertThat(JsonPath.<Boolean>read(responseBody, "$.paths['/api/v1/transfers'].post.parameters[0].required")).isTrue();
		assertThat(JsonPath.<String>read(responseBody, "$.paths['/api/v1/transfers'].post.parameters[0].name"))
			.isEqualTo("Idempotency-Key");
		assertThat(JsonPath.<String>read(responseBody, "$.components.securitySchemes.apiKeyAuth.type")).isEqualTo("apiKey");
		assertThat(JsonPath.<String>read(responseBody, "$.components.securitySchemes.apiKeyAuth.in")).isEqualTo("header");
		assertThat(JsonPath.<String>read(responseBody, "$.components.securitySchemes.apiKeyAuth.name")).isEqualTo("X-API-Key");
		assertThat(JsonPath.<List<String>>read(responseBody, "$.paths['/api/v1/wallets'].post.security[0].apiKeyAuth")).isEmpty();
	}

	@Test
	void allowsSwaggerUiWithoutApiKey() throws Exception {
		this.mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}

	@Test
	void allowsActuatorHealthWithoutApiKey() throws Exception {
		this.mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").isNotEmpty());
	}

	@Test
	void rejectsMissingApiKeyForProtectedEndpoint() throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "customer-123",
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("MISSING_API_KEY"))
			.andExpect(jsonPath("$.message").value("X-API-Key header is required"))
			.andExpect(jsonPath("$.details[0].field").value("X-API-Key"))
			.andExpect(jsonPath("$.details[0].message").value("X-API-Key header is required"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsInvalidApiKeyForProtectedEndpoint() throws Exception {
		this.mockMvc.perform(post("/api/v1/wallets")
			.header("X-API-Key", "wrong-key")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "customer-123",
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errorCode").value("INVALID_API_KEY"))
			.andExpect(jsonPath("$.message").value("X-API-Key is invalid"))
			.andExpect(jsonPath("$.details[0].field").value("X-API-Key"))
			.andExpect(jsonPath("$.details[0].message").value("X-API-Key is invalid"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsMissingOwnerReference() throws Exception {
		this.mockMvc.perform(apiPost("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.details[0].field").value("ownerReference"))
			.andExpect(jsonPath("$.details[0].message").value("ownerReference is required"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsUnsupportedCurrency() throws Exception {
		this.mockMvc.perform(apiPost("/api/v1/wallets")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "ownerReference": "customer-123",
				  "currency": "USD"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_CURRENCY"))
			.andExpect(jsonPath("$.message").value("currency is unsupported"))
			.andExpect(jsonPath("$.details[0].field").value("currency"))
			.andExpect(jsonPath("$.details[0].message").value("currency is unsupported"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void initializesWalletWithZeroAvailableBalanceAndActiveStatus() throws Exception {
		this.mockMvc.perform(apiPost("/api/v1/wallets")
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

		MvcResult result = this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
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

		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "amount": 10.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("MISSING_IDEMPOTENCY_KEY"))
			.andExpect(jsonPath("$.message").value("Idempotency-Key header is required"))
			.andExpect(jsonPath("$.details[0].field").value("Idempotency-Key"))
			.andExpect(jsonPath("$.details[0].message").value("Idempotency-Key header is required"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsInvalidAmount() throws Exception {
		UUID walletId = createWallet("depositor-125");

		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-002")
			.content("""
				{
				  "amount": 0,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("Validation failed"))
			.andExpect(jsonPath("$.details[0].field").value("amount"))
			.andExpect(jsonPath("$.details[0].message").value("amount must be positive"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsUnsupportedDepositCurrency() throws Exception {
		UUID walletId = createWallet("depositor-126");

		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-003")
			.content("""
				{
				  "amount": 10.00,
				  "currency": "USD"
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_CURRENCY"))
			.andExpect(jsonPath("$.message").value("currency is unsupported"))
			.andExpect(jsonPath("$.details[0].field").value("currency"))
			.andExpect(jsonPath("$.details[0].message").value("currency is unsupported"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsDepositWhenWalletIsMissing() throws Exception {
		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", UUID.randomUUID())
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "deposit-004")
			.content("""
				{
				  "amount": 10.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("WALLET_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("wallet not found"))
			.andExpect(jsonPath("$.details[0].field").value("walletId"))
			.andExpect(jsonPath("$.details[0].message").value("wallet not found"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void reusesOriginalResultForDuplicateIdempotencyKey() throws Exception {
		UUID walletId = createWallet("depositor-127");

		MvcResult firstResult = this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
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

		MvcResult secondResult = this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
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

		MvcResult result = this.mockMvc.perform(apiPost("/api/v1/transfers")
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

		this.mockMvc.perform(apiPost("/api/v1/transfers")
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
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"))
			.andExpect(jsonPath("$.message").value("insufficient available balance"))
			.andExpect(jsonPath("$.details[0].field").value("amount"))
			.andExpect(jsonPath("$.details[0].message").value("insufficient available balance"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsTransferWhenSourceAndDestinationAreTheSame() throws Exception {
		UUID walletId = createWallet("same-wallet");
		deposit(walletId, "seed-same-wallet", "50.00");

		this.mockMvc.perform(apiPost("/api/v1/transfers")
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
			.andExpect(jsonPath("$.errorCode").value("INVALID_TRANSFER_TARGET"))
			.andExpect(jsonPath("$.message").value("source and destination wallets must be different"))
			.andExpect(jsonPath("$.details[0].field").value("destinationWalletId"))
			.andExpect(jsonPath("$.details[0].message").value("source and destination wallets must be different"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsTransferWhenIdempotencyKeyIsMissing() throws Exception {
		UUID sourceWalletId = createWallet("source-003");
		UUID destinationWalletId = createWallet("destination-003");
		deposit(sourceWalletId, "seed-source-003", "30.00");

		this.mockMvc.perform(apiPost("/api/v1/transfers")
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
			.andExpect(jsonPath("$.errorCode").value("MISSING_IDEMPOTENCY_KEY"))
			.andExpect(jsonPath("$.message").value("Idempotency-Key header is required"))
			.andExpect(jsonPath("$.details[0].field").value("Idempotency-Key"))
			.andExpect(jsonPath("$.details[0].message").value("Idempotency-Key header is required"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsTransferWhenCurrencyIsUnsupported() throws Exception {
		UUID sourceWalletId = createWallet("source-004");
		UUID destinationWalletId = createWallet("destination-004");
		deposit(sourceWalletId, "seed-source-004", "30.00");

		this.mockMvc.perform(apiPost("/api/v1/transfers")
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
			.andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_CURRENCY"))
			.andExpect(jsonPath("$.message").value("currency is unsupported"))
			.andExpect(jsonPath("$.details[0].field").value("currency"))
			.andExpect(jsonPath("$.details[0].message").value("currency is unsupported"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void rejectsTransferWhenWalletIsMissing() throws Exception {
		UUID sourceWalletId = createWallet("source-005");
		deposit(sourceWalletId, "seed-source-005", "30.00");

		this.mockMvc.perform(apiPost("/api/v1/transfers")
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
			.andExpect(jsonPath("$.errorCode").value("WALLET_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("wallet not found"))
			.andExpect(jsonPath("$.details[0].field").value("destinationWalletId"))
			.andExpect(jsonPath("$.details[0].message").value("wallet not found"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void reusesOriginalResultForDuplicateTransferIdempotencyKey() throws Exception {
		UUID sourceWalletId = createWallet("source-006");
		UUID destinationWalletId = createWallet("destination-006");
		deposit(sourceWalletId, "seed-source-006", "120.00");

		MvcResult firstResult = this.mockMvc.perform(apiPost("/api/v1/transfers")
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

		MvcResult secondResult = this.mockMvc.perform(apiPost("/api/v1/transfers")
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

	@Test
	void allowsOnlyOneSuccessfulConcurrentTransferWhenCombinedAmountExceedsBalance() throws Exception {
		UUID sourceWalletId = createWallet("source-concurrent");
		UUID firstDestinationWalletId = createWallet("destination-concurrent-001");
		UUID secondDestinationWalletId = createWallet("destination-concurrent-002");
		deposit(sourceWalletId, "seed-concurrent-source", "1000.00");

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch start = new CountDownLatch(1);

			Callable<TransferAttemptResult> firstTransfer = concurrentTransfer(
				ready,
				start,
				"transfer-concurrent-800",
				sourceWalletId,
				firstDestinationWalletId,
				"800.00"
			);
			Callable<TransferAttemptResult> secondTransfer = concurrentTransfer(
				ready,
				start,
				"transfer-concurrent-700",
				sourceWalletId,
				secondDestinationWalletId,
				"700.00"
			);

			Future<TransferAttemptResult> firstFuture = executor.submit(firstTransfer);
			Future<TransferAttemptResult> secondFuture = executor.submit(secondTransfer);

			ready.await();
			start.countDown();

			List<TransferAttemptResult> results = List.of(firstFuture.get(), secondFuture.get());
			long successCount = results.stream().filter(result -> result.statusCode() == 201).count();
			long failureCount = results.stream().filter(result -> result.statusCode() == 409).count();

			assertThat(successCount).isEqualTo(1);
			assertThat(failureCount).isEqualTo(1);

			TransferAttemptResult failedTransfer = results.stream()
				.filter(result -> result.statusCode() == 409)
				.findFirst()
				.orElseThrow();
			assertThat(JsonPath.<String>read(failedTransfer.responseBody(), "$.errorCode")).isEqualTo("INSUFFICIENT_BALANCE");
			assertThat(JsonPath.<String>read(failedTransfer.responseBody(), "$.details[0].field")).isEqualTo("amount");
			assertThat(JsonPath.<String>read(failedTransfer.responseBody(), "$.details[0].message")).isEqualTo("insufficient available balance");

			Wallet sourceWallet = this.walletRepository.findById(sourceWalletId).orElseThrow();
			assertThat(sourceWallet.getAvailableBalance()).isIn(
				new java.math.BigDecimal("200.00"),
				new java.math.BigDecimal("300.00")
			);
			assertThat(sourceWallet.getAvailableBalance()).isGreaterThanOrEqualTo(new java.math.BigDecimal("0.00"));

			TransferAttemptResult successfulTransfer = results.stream()
				.filter(result -> result.statusCode() == 201)
				.findFirst()
				.orElseThrow();
			UUID transactionId = UUID.fromString(JsonPath.read(successfulTransfer.responseBody(), "$.transactionId"));
			List<WalletTransaction> transferTransactions = this.walletTransactionRepository.findAll().stream()
				.filter(transaction -> transaction.getTransactionType() == WalletTransactionType.TRANSFER)
				.toList();
			assertThat(transferTransactions).hasSize(1);
			assertThat(transferTransactions.getFirst().getId()).isEqualTo(transactionId);

			List<LedgerEntry> transferEntries = new ArrayList<>(this.ledgerEntryRepository.findAllByTransactionId(transactionId));
			assertThat(transferEntries).hasSize(2);
			assertThat(transferEntries.stream().map(LedgerEntry::getEntryType))
				.containsExactlyInAnyOrder(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT);
		}
	}

	@Test
	void returnsDepositInWalletHistoryAsCredit() throws Exception {
		UUID walletId = createWallet("history-deposit-wallet");
		UUID depositTransactionId = depositAndReturnTransactionId(walletId, "history-deposit-001", "125.00");

		this.mockMvc.perform(apiGet("/api/v1/wallets/{walletId}/transactions", walletId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].transactionId").value(depositTransactionId.toString()))
			.andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
			.andExpect(jsonPath("$.content[0].status").value("SUCCEEDED"))
			.andExpect(jsonPath("$.content[0].direction").value("CREDIT"))
			.andExpect(jsonPath("$.content[0].amount").value(125.0))
			.andExpect(jsonPath("$.content[0].currency").value("INR"))
			.andExpect(jsonPath("$.content[0].counterpartyWalletId").doesNotExist())
			.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
	}

	@Test
	void returnsOutgoingTransferAsDebitAndIncomingTransferAsCredit() throws Exception {
		UUID sourceWalletId = createWallet("history-source-wallet");
		UUID destinationWalletId = createWallet("history-destination-wallet");
		deposit(sourceWalletId, "history-seed-source", "200.00");
		UUID transferTransactionId = transfer("history-transfer-001", sourceWalletId, destinationWalletId, "80.00");

		this.mockMvc.perform(apiGet("/api/v1/wallets/{walletId}/transactions", sourceWalletId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].transactionId").value(transferTransactionId.toString()))
			.andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
			.andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
			.andExpect(jsonPath("$.content[0].counterpartyWalletId").value(destinationWalletId.toString()))
			.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());

		this.mockMvc.perform(apiGet("/api/v1/wallets/{walletId}/transactions", destinationWalletId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].transactionId").value(transferTransactionId.toString()))
			.andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
			.andExpect(jsonPath("$.content[0].direction").value("CREDIT"))
			.andExpect(jsonPath("$.content[0].counterpartyWalletId").value(sourceWalletId.toString()))
			.andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
	}

	@Test
	void paginatesWalletTransactionHistory() throws Exception {
		UUID walletId = createWallet("history-pagination-wallet");
		deposit(walletId, "history-page-001", "10.00");
		deposit(walletId, "history-page-002", "20.00");
		deposit(walletId, "history-page-003", "30.00");

		this.mockMvc.perform(apiGet("/api/v1/wallets/{walletId}/transactions", walletId)
			.param("page", "1")
			.param("size", "2")
			.param("sort", "amount,asc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].amount").value(30.0))
			.andExpect(jsonPath("$.content[0].direction").value("CREDIT"));
	}

	@Test
	void returnsNotFoundForMissingWalletTransactionHistory() throws Exception {
		this.mockMvc.perform(apiGet("/api/v1/wallets/{walletId}/transactions", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("WALLET_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("wallet not found"))
			.andExpect(jsonPath("$.details[0].field").value("walletId"))
			.andExpect(jsonPath("$.details[0].message").value("wallet not found"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void passesReconciliationForHealthyAccountingData() throws Exception {
		UUID sourceWalletId = createWallet("recon-source-001");
		UUID destinationWalletId = createWallet("recon-destination-001");
		deposit(sourceWalletId, "recon-seed-001", "250.00");
		transfer("recon-transfer-001", sourceWalletId, destinationWalletId, "100.00");

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PASS"))
			.andExpect(jsonPath("$.failedChecks").isEmpty());
	}

	@Test
	void failsReconciliationForCorruptedWalletBalance() throws Exception {
		UUID walletId = createWallet("recon-wallet-mismatch");
		deposit(walletId, "recon-seed-002", "125.00");
		this.jdbcTemplate.update(
			"update wallets set available_balance = ? where id = ?",
			new java.math.BigDecimal("135.00"),
			walletId
		);

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAIL"))
			.andExpect(jsonPath("$.failedChecks[0].checkType").value("WALLET_BALANCE_MISMATCH"))
			.andExpect(jsonPath("$.failedChecks[0].entityType").value("WALLET"))
			.andExpect(jsonPath("$.failedChecks[0].entityId").value(walletId.toString()));
	}

	@Test
	void failsReconciliationForMissingLedgerEntry() throws Exception {
		UUID sourceWalletId = createWallet("recon-source-002");
		UUID destinationWalletId = createWallet("recon-destination-002");
		deposit(sourceWalletId, "recon-seed-003", "200.00");
		UUID transactionId = transfer("recon-transfer-002", sourceWalletId, destinationWalletId, "80.00");

		LedgerEntry creditedEntry = this.ledgerEntryRepository.findAllByTransactionId(transactionId).stream()
			.filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
			.findFirst()
			.orElseThrow();
		this.jdbcTemplate.update("delete from ledger_entries where id = ?", creditedEntry.getId());

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAIL"))
			.andExpect(jsonPath("$.failedChecks[*].checkType").value(org.hamcrest.Matchers.hasItem("TRANSFER_LEDGER_STRUCTURE_MISMATCH")))
			.andExpect(jsonPath("$.failedChecks[*].entityId").value(org.hamcrest.Matchers.hasItem(transactionId.toString())));
	}

	@Test
	void failsReconciliationForUnbalancedTransferLedgerEntries() throws Exception {
		UUID sourceWalletId = createWallet("recon-source-003");
		UUID destinationWalletId = createWallet("recon-destination-003");
		deposit(sourceWalletId, "recon-seed-004", "220.00");
		UUID transactionId = transfer("recon-transfer-003", sourceWalletId, destinationWalletId, "90.00");

		LedgerEntry debitEntry = this.ledgerEntryRepository.findAllByTransactionId(transactionId).stream()
			.filter(entry -> entry.getEntryType() == LedgerEntryType.DEBIT)
			.findFirst()
			.orElseThrow();
		this.jdbcTemplate.update(
			"update ledger_entries set amount = ? where id = ?",
			new java.math.BigDecimal("95.00"),
			debitEntry.getId()
		);

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAIL"))
			.andExpect(jsonPath("$.failedChecks[*].checkType").value(org.hamcrest.Matchers.hasItem("TRANSFER_LEDGER_AMOUNT_MISMATCH")))
			.andExpect(jsonPath("$.failedChecks[*].entityId").value(org.hamcrest.Matchers.hasItem(transactionId.toString())));
	}

	@Test
	void failsReconciliationForDepositMissingCreditLedgerEntry() throws Exception {
		UUID walletId = createWallet("recon-deposit-wallet-001");
		UUID transactionId = depositAndReturnTransactionId(walletId, "recon-deposit-001", "75.00");

		LedgerEntry creditEntry = this.ledgerEntryRepository.findAllByTransactionId(transactionId).stream()
			.filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
			.findFirst()
			.orElseThrow();
		this.jdbcTemplate.update("delete from ledger_entries where id = ?", creditEntry.getId());

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAIL"))
			.andExpect(jsonPath("$.failedChecks[*].checkType").value(org.hamcrest.Matchers.hasItem("DEPOSIT_LEDGER_STRUCTURE_MISMATCH")))
			.andExpect(jsonPath("$.failedChecks[*].entityId").value(org.hamcrest.Matchers.hasItem(transactionId.toString())));
	}

	@Test
	void createsAuditLogsForWalletCreationDepositAndTransferAndSupportsAuditLogFiltering() throws Exception {
		UUID sourceWalletId = createWallet("audit-source-001");
		deposit(sourceWalletId, "audit-deposit-001", "150.00");
		UUID destinationWalletId = createWallet("audit-destination-001");
		UUID transferTransactionId = transfer("audit-transfer-001", sourceWalletId, destinationWalletId, "40.00");

		assertThat(this.auditLogRepository.findAll().stream().map(AuditLog::getAction))
			.contains(
				AuditAction.WALLET_CREATED,
				AuditAction.DEPOSIT_SUCCEEDED,
				AuditAction.TRANSFER_SUCCEEDED
			);

		this.mockMvc.perform(apiGet("/api/v1/audit-logs")
			.param("entityType", "TRANSACTION")
			.param("entityId", transferTransactionId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].action").value("TRANSFER_SUCCEEDED"))
			.andExpect(jsonPath("$[0].entityType").value("TRANSACTION"))
			.andExpect(jsonPath("$[0].entityId").value(transferTransactionId.toString()));
	}

	@Test
	void createsOutboxEventsForWalletCreationDepositAndTransferAndSupportsInspectionEndpoint() throws Exception {
		UUID sourceWalletId = createWallet("outbox-source-001");
		deposit(sourceWalletId, "outbox-deposit-001", "150.00");
		UUID destinationWalletId = createWallet("outbox-destination-001");
		UUID transferTransactionId = transfer("outbox-transfer-001", sourceWalletId, destinationWalletId, "40.00");

		assertThat(this.outboxEventRepository.findAll().stream().map(OutboxEvent::getEventType))
			.contains(
				OutboxEventType.WALLET_CREATED,
				OutboxEventType.DEPOSIT_SUCCEEDED,
				OutboxEventType.TRANSFER_SUCCEEDED
			);

		OutboxEvent transferEvent = this.outboxEventRepository.findAll().stream()
			.filter(event -> event.getEventType() == OutboxEventType.TRANSFER_SUCCEEDED)
			.filter(event -> event.getAggregateId().equals(transferTransactionId.toString()))
			.findFirst()
			.orElseThrow();
		assertThat(transferEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

		this.mockMvc.perform(apiGet("/api/v1/outbox-events"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].eventType").value(org.hamcrest.Matchers.hasItems(
				"WALLET_CREATED",
				"DEPOSIT_SUCCEEDED",
				"TRANSFER_SUCCEEDED"
			)))
			.andExpect(jsonPath("$[*].aggregateId").value(org.hamcrest.Matchers.hasItem(transferTransactionId.toString())));
	}

	@Test
	void createsAuditLogsForDuplicateIdempotencyRequestsAndFailedTransfer() throws Exception {
		UUID depositWalletId = createWallet("audit-depositor-duplicate");

		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", depositWalletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "audit-deposit-duplicate")
			.content("""
				{
				  "amount": 70.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated());
		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", depositWalletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "audit-deposit-duplicate")
			.content("""
				{
				  "amount": 70.00,
				  "currency": "INR"
				}
				"""))
			.andExpect(status().isCreated());

		UUID sourceWalletId = createWallet("audit-source-002");
		UUID destinationWalletId = createWallet("audit-destination-002");
		deposit(sourceWalletId, "audit-seed-source-002", "100.00");
		this.mockMvc.perform(apiPost("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "audit-transfer-duplicate")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 20.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isCreated());
		this.mockMvc.perform(apiPost("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "audit-transfer-duplicate")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 20.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isCreated());

		UUID failedSourceWalletId = createWallet("audit-source-failed");
		UUID failedDestinationWalletId = createWallet("audit-destination-failed");
		this.mockMvc.perform(apiPost("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "audit-transfer-insufficient")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "INR"
				}
				""".formatted(failedSourceWalletId, failedDestinationWalletId)))
			.andExpect(status().isConflict());

		assertThat(this.auditLogRepository.findAll().stream().map(AuditLog::getAction))
			.contains(
				AuditAction.DEPOSIT_DUPLICATE_REPLAY,
				AuditAction.TRANSFER_DUPLICATE_REPLAY,
				AuditAction.TRANSFER_INSUFFICIENT_BALANCE
			);
	}

	@Test
	void createsFailureOutboxEventForInsufficientBalanceTransfer() throws Exception {
		UUID sourceWalletId = createWallet("outbox-source-failed");
		UUID destinationWalletId = createWallet("outbox-destination-failed");

		this.mockMvc.perform(apiPost("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "outbox-transfer-insufficient")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 10.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isConflict());

		OutboxEvent failureEvent = this.outboxEventRepository.findAll().stream()
			.filter(event -> event.getEventType() == OutboxEventType.TRANSFER_INSUFFICIENT_BALANCE)
			.findFirst()
			.orElseThrow();
		assertThat(failureEvent.getAggregateType()).isEqualTo(OutboxAggregateType.WALLET);
		assertThat(failureEvent.getAggregateId()).isEqualTo(sourceWalletId.toString());
		assertThat(failureEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
		assertThat(failureEvent.getPayload())
			.containsEntry("sourceWalletId", sourceWalletId.toString())
			.containsEntry("destinationWalletId", destinationWalletId.toString())
			.containsEntry("currency", "INR");
	}

	@Test
	void createsAuditLogsForReconciliationRunsAndFailures() throws Exception {
		UUID walletId = createWallet("audit-reconciliation-wallet");
		deposit(walletId, "audit-reconciliation-seed", "90.00");

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PASS"));

		this.jdbcTemplate.update(
			"update wallets set available_balance = ? where id = ?",
			new java.math.BigDecimal("95.00"),
			walletId
		);

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAIL"));

		assertThat(this.auditLogRepository.findAll().stream().map(AuditLog::getAction))
			.contains(AuditAction.RECONCILIATION_RUN, AuditAction.RECONCILIATION_FAILED);
	}

	@Test
	void createsOutboxEventForFailedReconciliationRun() throws Exception {
		UUID walletId = createWallet("outbox-reconciliation-wallet");
		deposit(walletId, "outbox-reconciliation-seed", "90.00");

		this.jdbcTemplate.update(
			"update wallets set available_balance = ? where id = ?",
			new java.math.BigDecimal("95.00"),
			walletId
		);

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAIL"));

		OutboxEvent reconciliationEvent = this.outboxEventRepository.findAll().stream()
			.filter(event -> event.getEventType() == OutboxEventType.RECONCILIATION_FAILED)
			.findFirst()
			.orElseThrow();
		assertThat(reconciliationEvent.getAggregateType()).isEqualTo(OutboxAggregateType.RECONCILIATION);
		assertThat(reconciliationEvent.getAggregateId()).isEqualTo("reconciliation");
		assertThat(reconciliationEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
	}

	@Test
	void publishesPendingOutboxEventsAndMarksThemAsPublished() throws Exception {
		createWallet("outbox-worker-wallet");

		OutboxEvent pendingEvent = this.outboxEventRepository.findAll().stream()
			.filter(event -> event.getEventType() == OutboxEventType.WALLET_CREATED)
			.findFirst()
			.orElseThrow();
		assertThat(pendingEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
		assertThat(pendingEvent.getPublishedAt()).isNull();

		this.outboxEventWorker.publishPendingEvents();

		OutboxEvent publishedEvent = this.outboxEventRepository.findById(pendingEvent.getId()).orElseThrow();
		assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
		assertThat(publishedEvent.getPublishedAt()).isNotNull();
		assertThat(publishedEvent.getAttemptCount()).isZero();
		assertThat(publishedEvent.getLastError()).isNull();
	}

	private Callable<TransferAttemptResult> concurrentTransfer(
		CountDownLatch ready,
		CountDownLatch start,
		String idempotencyKey,
		UUID sourceWalletId,
		UUID destinationWalletId,
		String amount
	) {
		return () -> {
			ready.countDown();
			start.await();

			MvcResult result = this.mockMvc.perform(apiPost("/api/v1/transfers")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Idempotency-Key", idempotencyKey)
				.content("""
					{
					  "sourceWalletId": "%s",
					  "destinationWalletId": "%s",
					  "amount": %s,
					  "currency": "INR"
					}
					""".formatted(sourceWalletId, destinationWalletId, amount)))
				.andReturn();

			return new TransferAttemptResult(
				result.getResponse().getStatus(),
				result.getResponse().getContentAsString()
			);
		};
	}

	private UUID transfer(String idempotencyKey, UUID sourceWalletId, UUID destinationWalletId, String amount) throws Exception {
		MvcResult result = this.mockMvc.perform(apiPost("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", idempotencyKey)
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": %s,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId, amount)))
			.andExpect(status().isCreated())
			.andReturn();

		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.transactionId"));
	}

	private void deposit(UUID walletId, String idempotencyKey, String amount) throws Exception {
		this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
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

	private UUID depositAndReturnTransactionId(UUID walletId, String idempotencyKey, String amount) throws Exception {
		MvcResult result = this.mockMvc.perform(apiPost("/api/v1/wallets/{walletId}/deposit", walletId)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", idempotencyKey)
			.content("""
				{
				  "amount": %s,
				  "currency": "INR"
				}
				""".formatted(amount)))
			.andExpect(status().isCreated())
			.andReturn();

		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.transactionId"));
	}

	private UUID createWallet(String ownerReference) throws Exception {
		MvcResult result = this.mockMvc.perform(apiPost("/api/v1/wallets")
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

	private MockHttpServletRequestBuilder apiPost(String urlTemplate, Object... uriVariables) {
		return post(urlTemplate, uriVariables).header("X-API-Key", TEST_API_KEY);
	}

	private MockHttpServletRequestBuilder apiGet(String urlTemplate, Object... uriVariables) {
		return get(urlTemplate, uriVariables).header("X-API-Key", TEST_API_KEY);
	}

	private record TransferAttemptResult(int statusCode, String responseBody) {
	}
}

package com.saurab.atomicledger.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.saurab.atomicledger.PostgresIntegrationTest;

@SpringBootTest(properties = {
	"atomicledger.scheduling.enabled=false",
	"atomicledger.security.api-key=test-api-key"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OperationalMetricsIntegrationTest extends PostgresIntegrationTest {

	private static final String TEST_API_KEY = "test-api-key";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private WalletTransactionRepository walletTransactionRepository;

	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxEventWorker outboxEventWorker;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		this.outboxEventRepository.deleteAll();
		this.auditLogRepository.deleteAll();
		this.ledgerEntryRepository.deleteAll();
		this.walletTransactionRepository.deleteAll();
		this.walletRepository.deleteAll();
	}

	@Test
	void exposesActuatorMetricsEndpoint() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/actuator/metrics"))
			.andExpect(status().isOk())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();

		assertThat(JsonPath.<java.util.List<String>>read(responseBody, "$.names"))
			.contains(
				OperationalMetrics.WALLETS_CREATED_TOTAL,
				OperationalMetrics.TRANSFER_PROCESSING_DURATION,
				OperationalMetrics.OUTBOX_EVENTS_PUBLISHED_TOTAL
			);
	}

	@Test
	void incrementsWalletDepositAndTransferMetrics() throws Exception {
		double walletsCreatedBefore = counterValue(OperationalMetrics.WALLETS_CREATED_TOTAL);
		double depositsSucceededBefore = counterValue(OperationalMetrics.DEPOSITS_SUCCEEDED_TOTAL);
		double depositDuplicateBefore = counterValue(OperationalMetrics.DEPOSIT_DUPLICATE_REPLAYS_TOTAL);
		double transfersSucceededBefore = counterValue(OperationalMetrics.TRANSFERS_SUCCEEDED_TOTAL);
		double transfersFailedBefore = counterValue(OperationalMetrics.TRANSFERS_FAILED_TOTAL);
		double transferDuplicateBefore = counterValue(OperationalMetrics.TRANSFER_DUPLICATE_REPLAYS_TOTAL);
		long transferTimerCountBefore = timerCount(OperationalMetrics.TRANSFER_PROCESSING_DURATION);

		UUID sourceWalletId = createWallet("metrics-source-wallet");
		UUID destinationWalletId = createWallet("metrics-destination-wallet");
		deposit(sourceWalletId, "metrics-deposit-001", "100.00");
		deposit(sourceWalletId, "metrics-deposit-001", "100.00");
		transfer("metrics-transfer-001", sourceWalletId, destinationWalletId, "25.00");
		transfer("metrics-transfer-001", sourceWalletId, destinationWalletId, "25.00");

		this.mockMvc.perform(apiPost("/api/v1/transfers")
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", "metrics-transfer-failed")
			.content("""
				{
				  "sourceWalletId": "%s",
				  "destinationWalletId": "%s",
				  "amount": 500.00,
				  "currency": "INR"
				}
				""".formatted(sourceWalletId, destinationWalletId)))
			.andExpect(status().isConflict());

		assertThat(counterValue(OperationalMetrics.WALLETS_CREATED_TOTAL) - walletsCreatedBefore).isEqualTo(2.0);
		assertThat(counterValue(OperationalMetrics.DEPOSITS_SUCCEEDED_TOTAL) - depositsSucceededBefore).isEqualTo(1.0);
		assertThat(counterValue(OperationalMetrics.DEPOSIT_DUPLICATE_REPLAYS_TOTAL) - depositDuplicateBefore).isEqualTo(1.0);
		assertThat(counterValue(OperationalMetrics.TRANSFERS_SUCCEEDED_TOTAL) - transfersSucceededBefore).isEqualTo(1.0);
		assertThat(counterValue(OperationalMetrics.TRANSFERS_FAILED_TOTAL) - transfersFailedBefore).isEqualTo(1.0);
		assertThat(counterValue(OperationalMetrics.TRANSFER_DUPLICATE_REPLAYS_TOTAL) - transferDuplicateBefore).isEqualTo(1.0);
		assertThat(timerCount(OperationalMetrics.TRANSFER_PROCESSING_DURATION) - transferTimerCountBefore).isGreaterThanOrEqualTo(3L);
	}

	@Test
	void incrementsReconciliationAndOutboxSuccessMetrics() throws Exception {
		double reconciliationRunsBefore = counterValue(OperationalMetrics.RECONCILIATION_RUNS_TOTAL);
		double reconciliationFailuresBefore = counterValue(OperationalMetrics.RECONCILIATION_FAILURES_TOTAL);
		double outboxPublishedBefore = counterValue(OperationalMetrics.OUTBOX_EVENTS_PUBLISHED_TOTAL);

		UUID walletId = createWallet("metrics-reconciliation-wallet");
		deposit(walletId, "metrics-reconciliation-seed", "90.00");

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk());

		this.jdbcTemplate.update(
			"update wallets set available_balance = ? where id = ?",
			new BigDecimal("95.00"),
			walletId
		);

		this.mockMvc.perform(apiPost("/api/v1/reconciliation/run"))
			.andExpect(status().isOk());

		this.outboxEventWorker.publishPendingEvents();

		assertThat(counterValue(OperationalMetrics.RECONCILIATION_RUNS_TOTAL) - reconciliationRunsBefore).isEqualTo(2.0);
		assertThat(counterValue(OperationalMetrics.RECONCILIATION_FAILURES_TOTAL) - reconciliationFailuresBefore).isEqualTo(1.0);
		assertThat(counterValue(OperationalMetrics.OUTBOX_EVENTS_PUBLISHED_TOTAL) - outboxPublishedBefore).isGreaterThanOrEqualTo(1.0);
	}

	private double counterValue(String meterName) {
		return this.meterRegistry.get(meterName).counter().count();
	}

	private long timerCount(String meterName) {
		return this.meterRegistry.get(meterName).timer().count();
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

	private void transfer(String idempotencyKey, UUID sourceWalletId, UUID destinationWalletId, String amount) throws Exception {
		this.mockMvc.perform(apiPost("/api/v1/transfers")
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
			.andExpect(status().isCreated());
	}

	private MockHttpServletRequestBuilder apiPost(String urlTemplate, Object... uriVariables) {
		return post(urlTemplate, uriVariables).header("X-API-Key", TEST_API_KEY);
	}
}

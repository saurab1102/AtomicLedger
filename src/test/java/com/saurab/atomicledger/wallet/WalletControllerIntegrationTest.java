package com.saurab.atomicledger.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import com.saurab.atomicledger.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WalletRepository walletRepository;

	@BeforeEach
	void setUp() {
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
}

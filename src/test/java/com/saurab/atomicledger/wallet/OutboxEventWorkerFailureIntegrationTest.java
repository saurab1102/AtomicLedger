package com.saurab.atomicledger.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.saurab.atomicledger.PostgresIntegrationTest;

@SpringBootTest(properties = "atomicledger.scheduling.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxEventWorkerFailureIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxEventWorker outboxEventWorker;

	@Autowired
	private MeterRegistry meterRegistry;

	@MockitoBean
	private OutboxEventPublisher outboxEventPublisher;

	@BeforeEach
	void setUp() {
		this.outboxEventRepository.deleteAll();
	}

	@Test
	void incrementsFailedOutboxMetricWhenPublishingThrows() {
		double failedPublishesBefore = this.meterRegistry.get(OperationalMetrics.OUTBOX_EVENTS_FAILED_TOTAL).counter().count();
		OutboxEvent pendingEvent = this.outboxEventRepository.save(new OutboxEvent(
			UUID.randomUUID(),
			OutboxEventType.WALLET_CREATED,
			OutboxAggregateType.WALLET,
			UUID.randomUUID().toString(),
			Map.of("walletId", "wallet-001"),
			OutboxEventStatus.PENDING,
			0,
			Instant.now(),
			null,
			null
		));
		doThrow(new IllegalStateException("publish failed")).when(this.outboxEventPublisher).publish(any(OutboxEvent.class));

		this.outboxEventWorker.publishPendingEvents();

		OutboxEvent failedEvent = this.outboxEventRepository.findById(pendingEvent.getId()).orElseThrow();
		assertThat(failedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
		assertThat(failedEvent.getAttemptCount()).isEqualTo(1);
		assertThat(failedEvent.getLastError()).isEqualTo("publish failed");
		assertThat(this.meterRegistry.get(OperationalMetrics.OUTBOX_EVENTS_FAILED_TOTAL).counter().count() - failedPublishesBefore)
			.isEqualTo(1.0);
	}
}

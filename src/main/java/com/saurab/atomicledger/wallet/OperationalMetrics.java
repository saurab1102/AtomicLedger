package com.saurab.atomicledger.wallet;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class OperationalMetrics {

	static final String WALLETS_CREATED_TOTAL = "wallets_created_total";
	static final String DEPOSITS_SUCCEEDED_TOTAL = "deposits_succeeded_total";
	static final String DEPOSIT_DUPLICATE_REPLAYS_TOTAL = "deposit_duplicate_replays_total";
	static final String TRANSFERS_SUCCEEDED_TOTAL = "transfers_succeeded_total";
	static final String TRANSFERS_FAILED_TOTAL = "transfers_failed_total";
	static final String TRANSFER_DUPLICATE_REPLAYS_TOTAL = "transfer_duplicate_replays_total";
	static final String RECONCILIATION_RUNS_TOTAL = "reconciliation_runs_total";
	static final String RECONCILIATION_FAILURES_TOTAL = "reconciliation_failures_total";
	static final String OUTBOX_EVENTS_PUBLISHED_TOTAL = "outbox_events_published_total";
	static final String OUTBOX_EVENTS_FAILED_TOTAL = "outbox_events_failed_total";
	static final String TRANSFER_PROCESSING_DURATION = "transfer_processing_duration";

	private final MeterRegistry meterRegistry;
	private final Counter walletsCreatedCounter;
	private final Counter depositsSucceededCounter;
	private final Counter depositDuplicateReplaysCounter;
	private final Counter transfersSucceededCounter;
	private final Counter transfersFailedCounter;
	private final Counter transferDuplicateReplaysCounter;
	private final Counter reconciliationRunsCounter;
	private final Counter reconciliationFailuresCounter;
	private final Counter outboxEventsPublishedCounter;
	private final Counter outboxEventsFailedCounter;
	private final Timer transferProcessingTimer;

	public OperationalMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.walletsCreatedCounter = meterRegistry.counter(WALLETS_CREATED_TOTAL);
		this.depositsSucceededCounter = meterRegistry.counter(DEPOSITS_SUCCEEDED_TOTAL);
		this.depositDuplicateReplaysCounter = meterRegistry.counter(DEPOSIT_DUPLICATE_REPLAYS_TOTAL);
		this.transfersSucceededCounter = meterRegistry.counter(TRANSFERS_SUCCEEDED_TOTAL);
		this.transfersFailedCounter = meterRegistry.counter(TRANSFERS_FAILED_TOTAL);
		this.transferDuplicateReplaysCounter = meterRegistry.counter(TRANSFER_DUPLICATE_REPLAYS_TOTAL);
		this.reconciliationRunsCounter = meterRegistry.counter(RECONCILIATION_RUNS_TOTAL);
		this.reconciliationFailuresCounter = meterRegistry.counter(RECONCILIATION_FAILURES_TOTAL);
		this.outboxEventsPublishedCounter = meterRegistry.counter(OUTBOX_EVENTS_PUBLISHED_TOTAL);
		this.outboxEventsFailedCounter = meterRegistry.counter(OUTBOX_EVENTS_FAILED_TOTAL);
		this.transferProcessingTimer = meterRegistry.timer(TRANSFER_PROCESSING_DURATION);
	}

	public void incrementWalletsCreated() {
		this.walletsCreatedCounter.increment();
	}

	public void incrementDepositsSucceeded() {
		this.depositsSucceededCounter.increment();
	}

	public void incrementDepositDuplicateReplays() {
		this.depositDuplicateReplaysCounter.increment();
	}

	public void incrementTransfersSucceeded() {
		this.transfersSucceededCounter.increment();
	}

	public void incrementTransfersFailed() {
		this.transfersFailedCounter.increment();
	}

	public void incrementTransferDuplicateReplays() {
		this.transferDuplicateReplaysCounter.increment();
	}

	public void incrementReconciliationRuns() {
		this.reconciliationRunsCounter.increment();
	}

	public void incrementReconciliationFailures() {
		this.reconciliationFailuresCounter.increment();
	}

	public void incrementOutboxEventsPublished() {
		this.outboxEventsPublishedCounter.increment();
	}

	public void incrementOutboxEventsFailed() {
		this.outboxEventsFailedCounter.increment();
	}

	public Timer.Sample startTransferProcessingSample() {
		return Timer.start(this.meterRegistry);
	}

	public void recordTransferProcessingDuration(Timer.Sample sample) {
		sample.stop(this.transferProcessingTimer);
	}
}

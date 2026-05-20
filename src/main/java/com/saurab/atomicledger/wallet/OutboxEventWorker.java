package com.saurab.atomicledger.wallet;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventWorker {

	private final OutboxEventRepository outboxEventRepository;
	private final OutboxEventPublisher outboxEventPublisher;

	public OutboxEventWorker(OutboxEventRepository outboxEventRepository, OutboxEventPublisher outboxEventPublisher) {
		this.outboxEventRepository = outboxEventRepository;
		this.outboxEventPublisher = outboxEventPublisher;
	}

	/**
	 * Polls retryable outbox rows and advances them to PUBLISHED only after the
	 * publish step succeeds. Failed attempts remain PENDING so later polls can retry.
	 */
	@Scheduled(fixedDelayString = "${atomicledger.outbox.scheduler.fixed-delay:5000}")
	@Transactional
	public void publishPendingEvents() {
		List<OutboxEvent> pendingEvents = this.outboxEventRepository.findAllByStatusForUpdate(OutboxEventStatus.PENDING);
		for (OutboxEvent pendingEvent : pendingEvents) {
			try {
				this.outboxEventPublisher.publish(pendingEvent);
				pendingEvent.markPublished(Instant.now());
			}
			catch (RuntimeException exception) {
				pendingEvent.recordPublishFailure(exception.getMessage());
			}
		}
	}
}

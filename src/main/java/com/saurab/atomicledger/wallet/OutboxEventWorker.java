package com.saurab.atomicledger.wallet;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventWorker.class);
	private final OutboxEventRepository outboxEventRepository;
	private final OutboxEventPublisher outboxEventPublisher;
	private final OperationalMetrics operationalMetrics;

	public OutboxEventWorker(
		OutboxEventRepository outboxEventRepository,
		OutboxEventPublisher outboxEventPublisher,
		OperationalMetrics operationalMetrics
	) {
		this.outboxEventRepository = outboxEventRepository;
		this.outboxEventPublisher = outboxEventPublisher;
		this.operationalMetrics = operationalMetrics;
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
				this.operationalMetrics.incrementOutboxEventsPublished();
				LOGGER.atInfo()
					.addKeyValue("outboxEventId", pendingEvent.getId())
					.addKeyValue("eventType", pendingEvent.getEventType().name())
					.log("outbox_event_published");
			}
			catch (RuntimeException exception) {
				pendingEvent.recordPublishFailure(exception.getMessage());
				this.operationalMetrics.incrementOutboxEventsFailed();
				LOGGER.atWarn()
					.addKeyValue("outboxEventId", pendingEvent.getId())
					.addKeyValue("eventType", pendingEvent.getEventType().name())
					.addKeyValue("error", exception.getMessage())
					.log("outbox_event_publish_failed");
			}
		}
	}
}

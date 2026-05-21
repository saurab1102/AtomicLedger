package com.saurab.atomicledger.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOutboxEventPublisher.class);

	@Override
	public void publish(OutboxEvent event) {
		LOGGER.atInfo()
			.addKeyValue("outboxEventId", event.getId())
			.addKeyValue("eventType", event.getEventType().name())
			.addKeyValue("aggregateType", event.getAggregateType().name())
			.addKeyValue("aggregateId", event.getAggregateId())
			.addKeyValue("payload", event.getPayload())
			.log("publishing_outbox_event");
	}
}

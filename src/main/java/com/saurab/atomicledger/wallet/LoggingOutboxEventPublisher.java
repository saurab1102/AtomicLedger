package com.saurab.atomicledger.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOutboxEventPublisher.class);

	@Override
	public void publish(OutboxEvent event) {
		LOGGER.info(
			"Publishing outbox event id={} type={} aggregateType={} aggregateId={} payload={}",
			event.getId(),
			event.getEventType(),
			event.getAggregateType(),
			event.getAggregateId(),
			event.getPayload()
		);
	}
}

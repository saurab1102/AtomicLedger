package com.saurab.atomicledger.wallet;

public interface OutboxEventPublisher {

	void publish(OutboxEvent event);
}

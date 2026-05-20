package com.saurab.atomicledger.wallet;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventService {

	private final OutboxEventRepository outboxEventRepository;

	public OutboxEventService(OutboxEventRepository outboxEventRepository) {
		this.outboxEventRepository = outboxEventRepository;
	}

	@Transactional
	public OutboxEvent recordInCurrentTransaction(
		OutboxEventType eventType,
		OutboxAggregateType aggregateType,
		String aggregateId,
		Map<String, Object> payload
	) {
		return this.outboxEventRepository.save(new OutboxEvent(
			UUID.randomUUID(),
			eventType,
			aggregateType,
			aggregateId,
			payload,
			OutboxEventStatus.PENDING,
			0,
			Instant.now(),
			null,
			null
		));
	}

	@Transactional(readOnly = true)
	public List<OutboxEvent> findAll() {
		return this.outboxEventRepository.findAllByOrderByCreatedAtDescIdDesc();
	}
}

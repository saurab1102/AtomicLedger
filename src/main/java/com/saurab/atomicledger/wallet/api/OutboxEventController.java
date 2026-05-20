package com.saurab.atomicledger.wallet.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.OutboxEvent;
import com.saurab.atomicledger.wallet.OutboxEventService;

@RestController
@RequestMapping("/api/v1/outbox-events")
public class OutboxEventController {

	private final OutboxEventService outboxEventService;

	public OutboxEventController(OutboxEventService outboxEventService) {
		this.outboxEventService = outboxEventService;
	}

	@GetMapping
	public List<OutboxEventResponse> list() {
		return this.outboxEventService.findAll().stream()
			.map(this::toResponse)
			.toList();
	}

	private OutboxEventResponse toResponse(OutboxEvent event) {
		return new OutboxEventResponse(
			event.getId(),
			event.getEventType().name(),
			event.getAggregateType().name(),
			event.getAggregateId(),
			event.getPayload(),
			event.getStatus().name(),
			event.getAttemptCount(),
			event.getCreatedAt(),
			event.getPublishedAt(),
			event.getLastError()
		);
	}
}

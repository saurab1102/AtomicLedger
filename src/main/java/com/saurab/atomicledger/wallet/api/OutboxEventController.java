package com.saurab.atomicledger.wallet.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saurab.atomicledger.wallet.OutboxEvent;
import com.saurab.atomicledger.wallet.OutboxEventService;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/outbox-events")
@Tag(name = "Outbox Events", description = "Outbox event inspection APIs.")
public class OutboxEventController {

	private final OutboxEventService outboxEventService;

	public OutboxEventController(OutboxEventService outboxEventService) {
		this.outboxEventService = outboxEventService;
	}

	@GetMapping
	@Operation(summary = "List outbox events", description = "Returns all outbox events ordered from newest to oldest.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "Outbox events returned successfully.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = OutboxEventResponse.class)))
		),
		@ApiResponse(
			responseCode = "401",
			description = "Missing or invalid API key.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
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

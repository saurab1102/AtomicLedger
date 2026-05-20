package com.saurab.atomicledger.wallet;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	List<OutboxEvent> findAllByOrderByCreatedAtDescIdDesc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select event
		from OutboxEvent event
		where event.status = :status
		order by event.createdAt asc, event.id asc
		""")
	List<OutboxEvent> findAllByStatusForUpdate(@Param("status") OutboxEventStatus status);
}

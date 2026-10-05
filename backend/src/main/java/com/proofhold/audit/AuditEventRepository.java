package com.proofhold.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    org.springframework.data.domain.Page<AuditEvent> findByItemIdOrderByAtAsc(
            Long itemId, org.springframework.data.domain.Pageable pageable);
}

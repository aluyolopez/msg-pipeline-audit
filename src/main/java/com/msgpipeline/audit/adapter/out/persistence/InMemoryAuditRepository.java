package com.msgpipeline.audit.adapter.out.persistence;

import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/** Repositorio en memoria para el perfil local. Simula DynamoDB UpdateItem. */
@Slf4j
@Repository
@Profile("local")
public class InMemoryAuditRepository implements MessageRepository {

    private static final Map<String, AuditEvent> store = new HashMap<>();

    @Override
    public AuditEvent updateStatus(AuditEvent auditEvent) {
        store.put(auditEvent.getMessageId(), auditEvent);
        log.info("[LOCAL] DynamoDB UpdateItem simulado [messageId={}]" +
                " [SET #st=COMPLETED, #pa={}]",
                auditEvent.getMessageId(), auditEvent.getProcessedAt());
        return auditEvent;
    }
}

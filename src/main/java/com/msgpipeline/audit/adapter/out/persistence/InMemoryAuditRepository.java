package com.msgpipeline.audit.adapter.out.persistence;

import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================
 * CLASE: InMemoryAuditRepository — Adaptador de Salida (Memoria)
 * CAPA: Infraestructura — Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Simula la actualización de DynamoDB en memoria para desarrollo local.
 *
 * @Profile("local"): Solo activo en perfil local.
 * =========================================================================
 */
@Slf4j
@Repository
@Profile("local")
public class InMemoryAuditRepository implements MessageRepository {

    private final Map<String, AuditEvent> storage = new ConcurrentHashMap<>();

    @Override
    public AuditEvent updateStatus(AuditEvent auditEvent) {
        log.info("[LOCAL] Actualizando status en memoria [messageId={}] [newStatus={}]",
                auditEvent.getMessageId(), auditEvent.getFinalStatus());

        storage.put(auditEvent.getMessageId(), auditEvent);

        log.info("[LOCAL] Status actualizado a {} [messageId={}]",
                auditEvent.getFinalStatus(), auditEvent.getMessageId());
        log.info("[LOCAL] En AWS real: UpdateItem DynamoDB con ConditionExpression");

        return auditEvent;
    }

    public List<AuditEvent> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
    }
}

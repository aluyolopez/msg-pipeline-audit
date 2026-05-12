package com.msgpipeline.audit.application.port.in;

import com.msgpipeline.audit.domain.model.AuditEvent;

/**
 * =========================================================================
 * INTERFAZ: AuditMessagePort -- Puerto de Entrada del Audit
 * CAPA: Aplicacion -- Puerto de Entrada (Input Port)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * IMPLEMENTACION: AuditMessageUseCase
 * CALLER: AuditHandler (Lambda/EventBridge)
 *
 * FLUJO (implementado en AuditMessageUseCase):
 *   1. Thread.sleep(15_000) -- delay simulado de auditoria
 *   2. DynamoDB UpdateItem: PENDING -> COMPLETED
 *      (con alias #st porque 'status' es palabra reservada)
 *   3. SNS Publish: notificacion de auditoria completada
 *
 * TIMEOUT DEL LAMBDA: debe ser > 20s (15s delay + overhead AWS)
 * =========================================================================
 */
public interface AuditMessagePort {
    AuditEvent auditMessage(AuditEvent auditEvent, String requestId);
}

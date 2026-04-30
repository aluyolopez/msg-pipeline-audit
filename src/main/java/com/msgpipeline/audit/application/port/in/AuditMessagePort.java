package com.msgpipeline.audit.application.port.in;

import com.msgpipeline.audit.domain.model.AuditEvent;

/**
 * =========================================================================
 * INTERFAZ: AuditMessagePort — Puerto de Entrada del Audit
 * CAPA: Aplicación — Puerto de Entrada
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato del caso de uso de auditoría de mensajes.
 *
 * AuditHandler (Lambda) llama a este puerto cuando recibe un evento EventBridge.
 * AuditMessageUseCase implementa la lógica de negocio del audit.
 * =========================================================================
 */
public interface AuditMessagePort {

    /**
     * Audita y completa el procesamiento de un mensaje.
     *
     * FLUJO INTERNO (implementado en AuditMessageUseCase):
     *   1. Esperar 15 segundos (delay de procesamiento simulado)
     *   2. Actualizar status en DynamoDB: PENDING → COMPLETED
     *   3. Publicar notificación en SNS (email al destinatario)
     *
     * @param auditEvent Evento con el messageId y datos del mensaje
     * @param requestId  ID del request Lambda (para trazabilidad)
     * @return           AuditEvent con finalStatus=COMPLETED y processedAt
     */
    AuditEvent auditMessage(AuditEvent auditEvent, String requestId);
}

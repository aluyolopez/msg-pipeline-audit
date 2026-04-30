package com.msgpipeline.audit.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: AuditEvent — Entidad del Dominio (Audit)
 * CAPA: Dominio — Modelo de Negocio
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Representa el evento recibido de EventBridge que el Audit Lambda procesa.
 *
 * DIFERENCIA CON Message (del Processor):
 *   - Message: entidad completa del Processor (todos los campos)
 *   - AuditEvent: datos mínimos del evento EventBridge para el Audit
 *     El Audit no necesita todos los campos del mensaje, solo los
 *     necesarios para actualizar DynamoDB y publicar en SNS.
 *
 * FLUJO DEL AUDIT (Sesión 05):
 *   1. AuditHandler recibe evento EventBridge
 *   2. Extrae datos → construye AuditEvent
 *   3. AuditMessageUseCase procesa:
 *      → Delay 15s (simulado)
 *      → UpdateItem DynamoDB (PENDING → COMPLETED)
 *      → Publish SNS (email de notificación)
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /**
     * ID del mensaje original (de DynamoDB).
     * Se usa como Partition Key para el UpdateItem.
     * CRÍTICO: sin este campo no se puede actualizar DynamoDB.
     */
    private String messageId;

    /**
     * Tipo de mensaje: EMAIL, SMS, PUSH_NOTIFICATION.
     * Se incluye en el email SNS de notificación.
     */
    private String messageType;

    /**
     * Email del destinatario original del mensaje.
     * Se incluye en el email SNS de notificación.
     */
    private String recipientEmail;

    /**
     * Source del evento EventBridge.
     * Debe ser: "msg-pipeline.processor"
     */
    private String source;

    /**
     * Tipo del evento EventBridge.
     * Debe ser: "MessageReceived"
     */
    private String detailType;

    /**
     * ID del evento asignado por EventBridge.
     * Para trazabilidad: del audit record al evento EventBridge.
     */
    private String eventId;

    /**
     * Timestamp ISO-8601 de cuando el Audit Lambda procesó el evento.
     * Asignado por AuditMessageUseCase en el momento del procesamiento.
     */
    private String processedAt;

    /**
     * Estado final asignado por el Audit Lambda.
     * Siempre: "COMPLETED" (si no hay error)
     */
    private String finalStatus;
}

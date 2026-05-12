package com.msgpipeline.audit.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: AuditEvent -- Entidad del Dominio (Audit)
 * CAPA: Dominio -- Modelo de Negocio
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Representa el evento de EventBridge que el Audit Lambda procesa.
 *
 * ORIGEN:
 *   EventBridge Rule: msg-pipeline-audit-rule-sesion-07
 *   Source:     com.msgpipeline.processor
 *   DetailType: MessageProcessed
 *
 * CICLO DE VIDA SESION 07:
 *   1. Processor publica evento MessageProcessed en EventBridge
 *   2. Rule captura y dispara este Lambda
 *   3. AuditHandler extrae datos del campo 'detail'
 *   4. AuditMessageUseCase: Thread.sleep(15s) -> DynamoDB COMPLETED -> SNS
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /**
     * ID del mensaje original (Partition Key en DynamoDB).
     * CRITICO: sin este campo no se puede hacer UpdateItem.
     */
    private String messageId;

    /** Tipo: EMAIL | SMS | PUSH_NOTIFICATION */
    private String messageType;

    /** Email del destinatario original del mensaje */
    private String recipientEmail;

    /** Email del usuario autenticado con Cognito JWT */
    private String userEmail;

    /** Source del evento EventBridge: "com.msgpipeline.processor" */
    private String source;

    /** DetailType del evento EventBridge: "MessageProcessed" */
    private String detailType;

    /** ID del evento asignado por EventBridge (para trazabilidad) */
    private String eventId;

    /**
     * Timestamp ISO-8601 de cuando el Audit Lambda proceso el evento.
     * Asignado por AuditMessageUseCase DESPUES del delay de 15 segundos.
     */
    private String processedAt;

    /**
     * Estado final asignado por el Audit Lambda.
     * Siempre "COMPLETED" si no hay error.
     */
    private String finalStatus;
}

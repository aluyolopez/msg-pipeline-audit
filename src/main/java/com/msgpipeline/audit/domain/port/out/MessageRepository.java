package com.msgpipeline.audit.domain.port.out;

import com.msgpipeline.audit.domain.model.AuditEvent;

/**
 * =========================================================================
 * INTERFAZ: MessageRepository — Puerto de Salida (DynamoDB)
 * CAPA: Dominio — Puerto de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Puerto de salida para actualizar el estado del mensaje en DynamoDB.
 *
 * OPERACIÓN: UpdateItem (no PutItem)
 *   - PutItem: crea o reemplaza el ítem completo
 *   - UpdateItem: modifica solo atributos específicos (status, processedAt)
 *
 * El Audit Lambda usa UpdateItem porque:
 *   1. El ítem ya existe (lo creó el Processor Lambda con PENDING)
 *   2. Solo necesita cambiar status y processedAt
 *   3. PutItem eliminaría los otros atributos del ítem
 *
 * IMPLEMENTACIONES:
 *   - DynamoAuditRepository  → perfil 'aws' (DynamoDB real)
 *   - InMemoryAuditRepository → perfil 'local' (en memoria)
 * =========================================================================
 */
public interface MessageRepository {

    /**
     * Actualiza el status de un mensaje existente en DynamoDB.
     *
     * En 'aws': UpdateItem en la tabla msg-pipeline-messages
     *   - Actualiza: status → COMPLETED
     *   - Actualiza: processedAt → timestamp actual
     * En 'local': actualiza el Map en memoria
     *
     * @param auditEvent AuditEvent con el messageId y datos del procesamiento
     * @return           El auditEvent con finalStatus=COMPLETED y processedAt asignado
     */
    AuditEvent updateStatus(AuditEvent auditEvent);
}

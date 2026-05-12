package com.msgpipeline.audit.domain.port.out;

import com.msgpipeline.audit.domain.model.AuditEvent;

/**
 * =========================================================================
 * INTERFAZ: MessageRepository -- Puerto de Salida (DynamoDB UpdateItem)
 * CAPA: Dominio -- Puerto de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * OPERACION: UpdateItem (NO PutItem)
 *   El item ya existe (lo creo el Processor con PENDING).
 *   Solo actualizamos status y processedAt.
 *
 * CRITICO -- ALIAS #st:
 *   'status' es palabra reservada en DynamoDB.
 *   El adaptador DEBE usar ExpressionAttributeNames:
 *     "#st" -> "status"
 *   Sin alias -> DynamoDB lanza ValidationException.
 *
 * IMPLEMENTACIONES:
 *   - DynamoAuditRepository   --> perfil 'aws'
 *   - InMemoryAuditRepository --> perfil 'local'
 * =========================================================================
 */
public interface MessageRepository {
    AuditEvent updateStatus(AuditEvent auditEvent);
}

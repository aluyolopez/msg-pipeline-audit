package com.msgpipeline.audit.adapter.out.persistence;

import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

/**
 * =========================================================================
 * CLASE: DynamoAuditRepository — Adaptador de Salida (DynamoDB UpdateItem)
 * CAPA: Infraestructura — Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Actualiza el status de un mensaje existente en DynamoDB.
 *
 * OPERACIÓN: UpdateItem (no PutItem)
 *   UpdateItem modifica SOLO los atributos especificados sin tocar los demás.
 *   El ítem original (creado por el Processor con PENDING) conserva todos
 *   sus atributos excepto status y processedAt que son actualizados.
 *
 * EXPRESSION DYNAMO:
 *   UpdateExpression: "SET #st = :status, #pa = :processedAt"
 *   ExpressionAttributeNames: alias para atributos con nombres reservados
 *   ExpressionAttributeValues: valores a asignar
 *
 * ¿POR QUÉ USAR ALIAS (#st, #pa) EN LUGAR DEL NOMBRE DIRECTO?
 *   DynamoDB tiene palabras reservadas (ej: "status", "name", "type").
 *   Si el nombre del atributo es una palabra reservada, DynamoDB lanza error.
 *   La solución es usar ExpressionAttributeNames como alias:
 *     #st → "status" (alias para evitar conflicto con palabra reservada)
 *     #pa → "processedAt"
 * =========================================================================
 */
@Slf4j
@Repository
@Profile("aws")
@RequiredArgsConstructor
public class DynamoAuditRepository implements MessageRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    /**
     * Actualiza status=COMPLETED y processedAt en DynamoDB.
     *
     * La operación UpdateItem es:
     *   - ATÓMICA: se ejecuta completa o no se ejecuta
     *   - CONDICIONAL (opcional): se puede condicionar a que status=PENDING
     *     para evitar actualizar mensajes ya completados
     *   - EFICIENTE: solo lee y escribe los atributos necesarios
     *
     * @param auditEvent AuditEvent con messageId, finalStatus y processedAt
     * @return           El mismo auditEvent (DynamoDB no retorna el ítem actualizado sin extra config)
     */
    @Override
    public AuditEvent updateStatus(AuditEvent auditEvent) {
        log.info("Actualizando status en DynamoDB [messageId={}] [tabla={}] [newStatus={}]",
                auditEvent.getMessageId(), tableName, auditEvent.getFinalStatus());

        // ── Construir la condición de la clave primaria ───────────────────
        //
        // Key: { "messageId": AttributeValue.fromS(messageId) }
        // Este es el Partition Key del ítem a actualizar.
        //
        Map<String, AttributeValue> key = Map.of(
                "messageId", AttributeValue.fromS(auditEvent.getMessageId())
        );

        // ── Construir la expresión de actualización ───────────────────────
        //
        // SET #st = :status, #pa = :processedAt, #fi = :finalStatus
        //
        // #st → "status" (alias para evitar conflicto con palabra reservada)
        // #pa → "processedAt"
        //
        Map<String, String> expressionAttributeNames = Map.of(
                "#st", "status",
                "#pa", "processedAt"
        );

        Map<String, AttributeValue> expressionAttributeValues = Map.of(
                ":status",      AttributeValue.fromS(auditEvent.getFinalStatus()),
                ":processedAt", AttributeValue.fromS(auditEvent.getProcessedAt())
        );

        // ── Ejecutar UpdateItem ───────────────────────────────────────────
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("SET #st = :status, #pa = :processedAt")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                // ReturnValues.ALL_NEW: retorna el ítem completo después de la actualización
                // Opcional — útil para logging pero añade una lectura extra
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(request);

        log.info("UpdateItem exitoso [messageId={}] [status={}] [processedAt={}]",
                auditEvent.getMessageId(),
                auditEvent.getFinalStatus(),
                auditEvent.getProcessedAt());

        // Verificar los valores retornados por DynamoDB
        if (response.hasAttributes()) {
            String statusGuardado = response.attributes().get("status") != null
                    ? response.attributes().get("status").s()
                    : "UNKNOWN";
            log.info("DynamoDB confirma status actualizado: {}", statusGuardado);
        }

        return auditEvent;
    }
}

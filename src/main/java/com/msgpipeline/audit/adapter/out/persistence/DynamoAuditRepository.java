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
 * CLASE: DynamoAuditRepository -- Adaptador de Salida (DynamoDB UpdateItem)
 * CAPA: Infraestructura -- Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("aws"): Solo activo en Lambda.
 *
 * CRITICO -- POR QUE USAR ALIAS #st:
 *   DynamoDB tiene palabras reservadas (status, name, type, etc.).
 *   Si 'status' aparece directamente en UpdateExpression, DynamoDB lanza:
 *     ValidationException: Value provided in ExpressionAttributeNames unused
 *   La solucion es ExpressionAttributeNames:
 *     "#st" -> "status"   (alias para la palabra reservada)
 *     "#pa" -> "processedAt"
 *   UpdateExpression: "SET #st = :status, #pa = :processedAt"
 *
 * OPERACION: UpdateItem (NO PutItem)
 *   Modifica SOLO status y processedAt del item existente.
 *   El item fue creado por el Processor con status=PENDING.
 * =========================================================================
 */
@Slf4j
@Repository
@Profile("aws")
@RequiredArgsConstructor
public class DynamoAuditRepository implements MessageRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Override
    public AuditEvent updateStatus(AuditEvent auditEvent) {
        log.info("DynamoDB UpdateItem [messageId={}] [tabla={}] [newStatus={}]",
                auditEvent.getMessageId(), tableName, auditEvent.getFinalStatus());

        // -- Clave primaria del item a actualizar ---------------------------
        Map<String, AttributeValue> key = Map.of(
                "messageId", AttributeValue.fromS(auditEvent.getMessageId())
        );

        // -- Alias de nombres (CRITICO: status es palabra reservada) --------
        Map<String, String> expressionAttributeNames = Map.of(
                "#st", "status",      // alias para 'status' (reservada)
                "#pa", "processedAt"
        );

        // -- Valores de la expresion de actualizacion -----------------------
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
                ":status",      AttributeValue.fromS(auditEvent.getFinalStatus()),
                ":processedAt", AttributeValue.fromS(auditEvent.getProcessedAt())
        );

        // -- Ejecutar UpdateItem --------------------------------------------
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("SET #st = :status, #pa = :processedAt")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(request);

        if (response.hasAttributes() && response.attributes().get("status") != null) {
            log.info("DynamoDB confirma status: {} [messageId={}]",
                    response.attributes().get("status").s(), auditEvent.getMessageId());
        }

        log.info("UpdateItem exitoso [messageId={}] [status=COMPLETED]",
                auditEvent.getMessageId());

        return auditEvent;
    }
}

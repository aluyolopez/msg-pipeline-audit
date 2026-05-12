package com.msgpipeline.audit.application.usecase;

import com.msgpipeline.audit.application.port.in.AuditMessagePort;
import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.MessageRepository;
import com.msgpipeline.audit.domain.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * =========================================================================
 * CLASE: AuditMessageUseCase -- Caso de Uso de Auditoria
 * CAPA: Aplicacion -- Caso de Uso (Application Service)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD (SRP): Orquesta delay + DynamoDB update + SNS notify.
 * No sabe de EventBridge, DynamoDB ni SNS directamente.
 *
 * FLUJO SESION 07:
 *   AuditHandler --> auditMessage()
 *   --> Paso 1: Thread.sleep(15_000) -- delay de auditoria
 *   --> Paso 2: DynamoDB UpdateItem PENDING -> COMPLETED
 *               CRITICO: alias #st porque 'status' es palabra reservada
 *   --> Paso 3: SNS Publish -- notificacion de auditoria completada
 *
 * POR QUE EL DELAY DE 15 SEGUNDOS:
 *   Simula el tiempo de auditoria real (llamadas externas, reportes, etc.)
 *   PROPOSITO PEDAGOGICO: observar en DynamoDB:
 *     status=PENDING mientras el Lambda espera
 *     status=COMPLETED tras el UpdateItem
 *
 * TIMEOUT DEL LAMBDA: DEBE ser > 20 segundos.
 *
 * PATRONES:
 *   - Use Case (Application Service)
 *   - Observer: SNS notifica sin saber los suscriptores
 *   - Template Method: flujo fijo de 3 pasos
 *   - DIP: depende de puertos (abstracciones)
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditMessageUseCase implements AuditMessagePort {

    // Puerto de salida -- DynamoDB (UpdateItem)
    // 'aws'   --> DynamoAuditRepository
    // 'local' --> InMemoryAuditRepository
    private final MessageRepository messageRepository;

    // Puerto de salida -- SNS (Publish)
    // 'aws'   --> SnsNotificationAdapter
    // 'local' --> InMemoryNotificationAdapter
    private final NotificationPort notificationPort;

    @Override
    public AuditEvent auditMessage(AuditEvent auditEvent, String requestId) {
        log.info("Iniciando auditoria [messageId={}] [requestId={}]",
                auditEvent.getMessageId(), requestId);

        // -- Paso 1: Delay de 15 segundos ----------------------------------
        // Simula el tiempo de auditoria asincrona.
        // En DynamoDB se puede observar: status=PENDING -> (15s) -> COMPLETED
        log.info("Iniciando delay de 15 segundos [messageId={}]", auditEvent.getMessageId());
        log.info("--> Verifique en DynamoDB que el status es PENDING");

        try {
            Thread.sleep(15_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrumpido [messageId={}]", auditEvent.getMessageId());
        }

        log.info("Delay completado. Actualizando DynamoDB [messageId={}]",
                auditEvent.getMessageId());

        // -- Paso 2: DynamoDB UpdateItem: PENDING -> COMPLETED --------------
        // CRITICO: usa alias #st porque 'status' es palabra reservada en DynamoDB
        // Sin alias -> DynamoDB lanza ValidationException
        auditEvent.setProcessedAt(Instant.now().toString());
        auditEvent.setFinalStatus("COMPLETED");

        AuditEvent updated = messageRepository.updateStatus(auditEvent);
        log.info("DynamoDB actualizado: PENDING -> COMPLETED [messageId={}] [processedAt={}]",
                updated.getMessageId(), updated.getProcessedAt());

        // -- Paso 3: SNS Publish (BEST-EFFORT) ------------------------------
        // PATRON OBSERVER: publica sin saber los suscriptores
        // Si SNS falla, el status COMPLETED ya fue guardado en DynamoDB
        try {
            notificationPort.notificarProcesamiento(updated);
            log.info("SNS Publish exitoso -- auditoria completada [messageId={}]",
                    updated.getMessageId());
        } catch (Exception e) {
            log.error("Error SNS Publish [messageId={}]: {}",
                    updated.getMessageId(), e.getMessage());
        }

        log.info("Auditoria completada [messageId={}] [status=COMPLETED]",
                updated.getMessageId());

        return updated;
    }
}

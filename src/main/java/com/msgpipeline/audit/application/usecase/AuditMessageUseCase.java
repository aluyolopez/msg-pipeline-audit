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
 * CLASE: AuditMessageUseCase — Caso de Uso de Auditoría
 * CAPA: Aplicación — Caso de Uso
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD (SRP — SOLID):
 *   Orquesta el flujo de auditoría: delay → DynamoDB update → SNS notify.
 *   No sabe de EventBridge, DynamoDB ni SNS directamente.
 *   Solo conoce los puertos (interfaces) del dominio.
 *
 * FLUJO DETALLADO (Sesión 05):
 *   EventBridge → AuditHandler → auditMessage()
 *   → Paso 1: Esperar 15 segundos (delay de procesamiento)
 *   → Paso 2: Actualizar DynamoDB PENDING → COMPLETED
 *   → Paso 3: Publicar notificación SNS (email)
 *
 * ¿POR QUÉ EL DELAY DE 15 SEGUNDOS?
 *   En un sistema real, el Audit Lambda haría trabajo real:
 *     - Validar el contenido del mensaje
 *     - Llamar a servicios externos de entrega (email gateway, SMS, etc.)
 *     - Registrar métricas de auditoría
 *   El delay de 15s simula este tiempo de procesamiento.
 *   También permite observar en la consola de AWS cómo el status
 *   permanece en PENDING y luego cambia a COMPLETED.
 *
 * TIMEOUT RECOMENDADO DEL LAMBDA AUDIT:
 *   Mínimo 30 segundos (15s delay + tiempo DynamoDB + tiempo SNS).
 *   Configurar en Lambda Console → General configuration → Timeout.
 *
 * PATRONES APLICADOS:
 *   - Use Case (Application Service): orquesta el flujo de negocio
 *   - Observer: publica notificación sin saber los suscriptores de SNS
 *   - Strategy: repository y notificationPort cambian por perfil activo
 *   - Dependency Injection: @RequiredArgsConstructor + @Service
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditMessageUseCase implements AuditMessagePort {

    // Puerto de salida — DynamoDB (UpdateItem)
    // 'local' → InMemoryAuditRepository
    // 'aws'   → DynamoAuditRepository
    private final MessageRepository messageRepository;

    // Puerto de salida — SNS (Publish)
    // 'local' → InMemoryNotificationAdapter
    // 'aws'   → SnsNotificationAdapter
    private final NotificationPort notificationPort;

    /**
     * Caso de uso: Auditar y completar el procesamiento del mensaje.
     *
     * NOTA IMPORTANTE SOBRE EL DELAY:
     *   Thread.sleep(15_000) es válido en Lambda porque:
     *   1. Lambda permite hasta 15 minutos de timeout
     *   2. Durante el sleep, Lambda no consume CPU activamente
     *   3. En un caso real, este sería tiempo de I/O (llamadas externas)
     *   4. El costo de Lambda se calcula por GB-ms activo — dormido sigue contando
     *   ALTERNATIVA REAL: usar Step Functions con Wait state (no consume tiempo en Lambda)
     *
     * @param auditEvent AuditEvent con datos del mensaje EventBridge
     * @param requestId  ID del request Lambda
     * @return           AuditEvent con status=COMPLETED
     */
    @Override
    public AuditEvent auditMessage(AuditEvent auditEvent, String requestId) {
        log.info("Iniciando auditoría [messageId={}] [requestId={}]",
                auditEvent.getMessageId(), requestId);

        // ── Paso 1: Simular delay de procesamiento (15 segundos) ──────────
        //
        // Este delay simula el tiempo que tomaría el procesamiento real:
        //   - Llamadas a servicios externos de entrega de mensajes
        //   - Validaciones contra bases de datos externas
        //   - Procesamiento de imágenes o archivos adjuntos
        //
        // PROPÓSITO PEDAGÓGICO:
        //   Permite observar en la consola de DynamoDB cómo el ítem
        //   permanece en status=PENDING durante 15 segundos, y luego
        //   cambia a status=COMPLETED. Esto demuestra el flujo asíncrono.
        //
        log.info("Iniciando delay de procesamiento de 15 segundos [messageId={}]",
                auditEvent.getMessageId());
        log.info("→ Mientras espera: verifique en DynamoDB que el status es PENDING");

        try {
            Thread.sleep(15_000);  // 15 segundos de delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrumpido [messageId={}]", auditEvent.getMessageId());
        }

        log.info("Delay completado. Procediendo a actualizar DynamoDB [messageId={}]",
                auditEvent.getMessageId());

        // ── Paso 2: Actualizar estado en DynamoDB ─────────────────────────
        //
        // UpdateItem: cambia solo status y processedAt en el ítem existente.
        // No reemplaza el ítem completo (eso sería PutItem).
        //
        // Antes: { status: "PENDING", ... }
        // Después: { status: "COMPLETED", processedAt: "2025-...", ... }
        //
        auditEvent.setProcessedAt(Instant.now().toString());
        auditEvent.setFinalStatus("COMPLETED");

        AuditEvent updated = messageRepository.updateStatus(auditEvent);

        log.info("DynamoDB actualizado: PENDING → COMPLETED [messageId={}] [processedAt={}]",
                updated.getMessageId(), updated.getProcessedAt());

        // ── Paso 3: Publicar notificación SNS ────────────────────────────
        //
        // PATRÓN OBSERVER:
        //   El Audit Lambda notifica sin saber quiénes están suscritos al SNS.
        //   Los suscriptores (email, SMS, otras Lambdas) reaccionan de forma
        //   independiente sin modificar el código del Audit Lambda.
        //
        // BEST-EFFORT: si SNS falla, el status ya fue actualizado a COMPLETED.
        //   No fallamos el Lambda entero por un error de notificación.
        //
        try {
            notificationPort.notificarProcesamiento(updated);
            log.info("Notificación SNS publicada [messageId={}]", updated.getMessageId());
        } catch (Exception e) {
            log.error("Error publicando notificación SNS [messageId={}]: {}",
                    updated.getMessageId(), e.getMessage(), e);
            // No relanzamos — el status COMPLETED ya fue guardado en DynamoDB
        }

        log.info("Auditoría completada [messageId={}] [status={}] [processedAt={}]",
                updated.getMessageId(), updated.getFinalStatus(), updated.getProcessedAt());

        return updated;
    }
}

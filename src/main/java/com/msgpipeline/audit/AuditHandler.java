package com.msgpipeline.audit;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.audit.application.port.in.AuditMessagePort;
import com.msgpipeline.audit.config.AuditApplication;
import com.msgpipeline.audit.domain.model.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * =========================================================================
 * CLASE: AuditHandler — Lambda Entry Point (EventBridge Consumer)
 * CAPA: Infraestructura — Adaptador de Entrada (Input Adapter)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * NUEVO EN SESIÓN 05: Lambda que consume eventos de EventBridge.
 *
 * FLUJO COMPLETO:
 *   ┌─────────────────────────────────┐
 *   │  EventBridge Bus                │
 *   │  msg-pipeline-events-sesion-05  │
 *   │  Rule: source=msg-pipeline.proc │
 *   └──────────────┬──────────────────┘
 *                  │ Target (Event Rule)
 *                  ▼
 *   ┌──────────────────────────────────────┐
 *   │  msg-pipeline-audit-sesion-05        │
 *   │  AuditHandler::handleRequest         │
 *   └────────────────┬─────────────────────┘
 *                    │
 *       ┌────────────┴──────────────────┐
 *       ▼                               ▼
 * ┌──────────────┐          ┌────────────────────────┐
 * │   DynamoDB   │          │  SNS                   │
 * │ UpdateItem   │          │  msg-pipeline-email-   │
 * │ COMPLETED    │          │  notifications-s05     │
 * │ (delay 15s)  │          │  Publish               │
 * └──────────────┘          └────────────────────────┘
 *
 * EVENTO DE EVENTBRIDGE RECIBIDO:
 * {
 *   "version": "0",
 *   "id": "event-uuid",
 *   "source": "msg-pipeline.processor",
 *   "detail-type": "MessageReceived",
 *   "detail": {
 *     "messageId": "uuid...",
 *     "messageType": "EMAIL",
 *     "recipientEmail": "...",
 *     "status": "PENDING",
 *     "timestamp": "..."
 *   }
 * }
 *
 * TIPO DE INPUT: Map<String, Object>
 *   EventBridge envía eventos como JSON genérico.
 *   Usamos Map<String, Object> para flexibilidad y evitar dependencias
 *   de clases específicas de eventos (ScheduledEvent, etc.).
 *   El SDK de Lambda serializa automáticamente el JSON a Map.
 *
 * HANDLER A CONFIGURAR EN LAMBDA CONSOLE:
 *   com.msgpipeline.audit.AuditHandler::handleRequest
 *
 * VARIABLES DE ENTORNO REQUERIDAS EN LAMBDA:
 *   DYNAMODB_TABLE_NAME = msg-pipeline-messages
 *   SNS_TOPIC_ARN       = arn:aws:sns:us-east-1:{ACCOUNT}:msg-pipeline-email-notifications-sesion-05
 *   AWS_REGION          = us-east-1 (Lambda la inyecta automáticamente)
 * =========================================================================
 */
@Slf4j
public class AuditHandler implements RequestHandler<Map<String, Object>, Void> {

    // ── Bloque static — Cold Start ────────────────────────────────────────
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AuditMessagePort auditMessagePort;

    static {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  AuditHandler — Inicialización Cold Start (Sesión 05)       ║");
        log.info("║  Trigger: EventBridge → DynamoDB (COMPLETED) + SNS Email   ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(AuditApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("aws")
                .run();

        auditMessagePort = context.getBean(AuditMessagePort.class);

        log.info("Contexto Spring inicializado. Puerto de entrada listo.");
        log.info("Tabla DynamoDB: {}",
                context.getEnvironment().getProperty("app.aws.dynamodb-table"));
        log.info("SNS Topic ARN: {}",
                context.getEnvironment().getProperty("app.aws.sns-topic-arn"));
    }

    public AuditHandler() {
        // Constructor sin argumentos — requerido por Lambda runtime
    }

    /**
     * handleRequest — Invocado por Lambda cuando EventBridge enruta un evento.
     *
     * ESTRUCTURA DEL EVENTO EVENTBRIDGE:
     * {
     *   "version": "0",
     *   "id": "12345678-...",
     *   "source": "msg-pipeline.processor",
     *   "detail-type": "MessageReceived",
     *   "account": "123456789012",
     *   "time": "2025-05-01T19:00:00Z",
     *   "region": "us-east-1",
     *   "detail": { ... datos del mensaje ... }
     * }
     *
     * TIMEOUT RECOMENDADO DE LAMBDA: 60 segundos
     * (para contemplar el delay de 15s + tiempo de DynamoDB y SNS)
     *
     * @param event   Evento de EventBridge como Map genérico
     * @param context Contexto Lambda (requestId, timeout restante, etc.)
     * @return        Void — Lambda no usa el valor de retorno
     */
    @Override
    public Void handleRequest(Map<String, Object> event, Context context) {
        log.info("Evento EventBridge recibido [requestId={}] [tiempoRestante={}ms]",
                context.getAwsRequestId(),
                context.getRemainingTimeInMillis());

        try {
            // ── Extraer el source del evento (para validación) ────────────
            String source = (String) event.get("source");
            String detailType = (String) event.get("detail-type");

            log.info("Evento [source={}] [detail-type={}]", source, detailType);

            // ── Validar que el evento es del tipo esperado ─────────────────
            if (!"msg-pipeline.processor".equals(source) ||
                    !"MessageReceived".equals(detailType)) {
                log.warn("Evento ignorado — source/detail-type no reconocido [source={}] [detailType={}]",
                        source, detailType);
                return null;
            }

            // ── Extraer el detail del evento ──────────────────────────────
            //
            // El "detail" es el payload JSON que envió el Processor Lambda.
            // AWS SDK lo deserializa automáticamente a Map<String, Object>.
            //
            @SuppressWarnings("unchecked")
            Map<String, Object> detail = (Map<String, Object>) event.get("detail");

            if (detail == null) {
                log.error("Evento EventBridge sin campo 'detail' — ignorando");
                return null;
            }

            String messageId = (String) detail.get("messageId");
            String messageType = (String) detail.getOrDefault("messageType", "UNKNOWN");
            String recipientEmail = (String) detail.getOrDefault("recipientEmail", "");

            log.info("Procesando evento [messageId={}] [tipo={}] [destinatario={}]",
                    messageId, messageType, recipientEmail);

            if (messageId == null || messageId.isBlank()) {
                log.error("Evento sin messageId — no se puede procesar");
                return null;
            }

            // ── Construir AuditEvent para el caso de uso ──────────────────
            AuditEvent auditEvent = AuditEvent.builder()
                    .messageId(messageId)
                    .messageType(messageType)
                    .recipientEmail(recipientEmail)
                    .source(source)
                    .detailType(detailType)
                    .eventId((String) event.getOrDefault("id", ""))
                    .build();

            // ── Ejecutar caso de uso ──────────────────────────────────────
            //
            // El caso de uso:
            //   1. Espera 15 segundos (delay simulado de procesamiento)
            //   2. Actualiza DynamoDB: PENDING → COMPLETED
            //   3. Publica notificación SNS (email)
            //
            auditMessagePort.auditMessage(auditEvent, context.getAwsRequestId());

            log.info("AuditHandler completado [messageId={}]", messageId);

        } catch (Exception e) {
            log.error("Error en AuditHandler [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            // No relanzamos la excepción:
            // EventBridge tiene retry automático si el Lambda falla.
            // Si hay error persistente, el mensaje quedará en PENDING
            // y se puede revisar en CloudWatch Logs.
            throw new RuntimeException("Error en AuditHandler: " + e.getMessage(), e);
        }

        return null;
    }
}

package com.msgpipeline.audit;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
 * CLASE: AuditHandler -- Lambda Entry Point (EventBridge Consumer)
 * CAPA: Infraestructura -- Adaptador de Entrada (Input Adapter)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * FLUJO SESION 07 -- POSICION DEL AUDIT LAMBDA:
 *   EventBridge Bus: msg-pipeline-events-sesion-07
 *   Rule: msg-pipeline-audit-rule-sesion-07
 *   Filtro: source=com.msgpipeline.processor + detail-type=MessageProcessed
 *   Target: Lambda msg-pipeline-audit-sesion-07
 *   --> AuditHandler::handleRequest(Map<String,Object>)
 *   --> Thread.sleep(15s) + DynamoDB UpdateItem(COMPLETED) + SNS Publish
 *
 * EVENTO EVENTBRIDGE RECIBIDO:
 * {
 *   "version": "0",
 *   "id": "event-uuid",
 *   "source": "com.msgpipeline.processor",
 *   "detail-type": "MessageProcessed",
 *   "detail": {
 *     "messageId": "uuid...",
 *     "messageType": "EMAIL",
 *     "recipientEmail": "...",
 *     "status": "PENDING",
 *     "processedAt": "...",
 *     "userEmail": "..."
 *   }
 * }
 *
 * HANDLER: com.msgpipeline.audit.AuditHandler::handleRequest
 * ENV:     DYNAMODB_TABLE_NAME, SNS_TOPIC_ARN
 * CRITICO -- TIMEOUT: > 20s (delay 15s + overhead). Recomendado: 60s.
 *
 * IMPORTANTE -- WebApplicationType.SERVLET:
 *   Requerido en Spring Boot 3.5. NONE causa ClassCastException.
 * =========================================================================
 */
@Slf4j
public class AuditHandler implements RequestHandler<Map<String, Object>, Void> {

    // -- Bloque static -- Cold Start -----------------------------------------
    private static final AuditMessagePort auditMessagePort;

    static {
        log.info("AuditHandler -- Cold Start (Sesion 07)");
        log.info("Trigger: EventBridge Rule (source=com.msgpipeline.processor, detail-type=MessageProcessed)");
        log.info("Acciones: Thread.sleep(15s) + DynamoDB UpdateItem(COMPLETED) + SNS Publish");

        // WebApplicationType.SERVLET: OBLIGATORIO en Spring Boot 3.5
        ConfigurableApplicationContext context = new SpringApplicationBuilder(AuditApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        auditMessagePort = context.getBean(AuditMessagePort.class);
        log.info("Tabla DynamoDB: {}", context.getEnvironment().getProperty("app.aws.dynamodb-table"));
        log.info("SNS Topic ARN: {}", context.getEnvironment().getProperty("app.aws.sns-topic-arn"));
    }

    /** Constructor publico sin argumentos -- OBLIGATORIO para AWS Lambda */
    public AuditHandler() { }

    /**
     * handleRequest -- Invocado por Lambda cuando EventBridge enruta el evento.
     *
     * ESTRUCTURA DEL EVENTO EVENTBRIDGE:
     * {
     *   "version": "0",
     *   "id": "uuid",
     *   "source": "com.msgpipeline.processor",
     *   "detail-type": "MessageProcessed",
     *   "account": "629742034427",
     *   "region": "us-east-1",
     *   "detail": { ...datos del mensaje... }
     * }
     */
    @Override
    @SuppressWarnings("unchecked")
    public Void handleRequest(Map<String, Object> event, Context context) {
        log.info("Evento EventBridge [requestId={}] [tiempoRestante={}ms]",
                context.getAwsRequestId(), context.getRemainingTimeInMillis());

        try {
            // -- Extraer y validar metadata del evento ----------------------
            String source     = (String) event.get("source");
            String detailType = (String) event.get("detail-type");

            log.info("Evento [source={}] [detail-type={}]", source, detailType);

            // Validar que el evento es del tipo esperado
            if (!"com.msgpipeline.processor".equals(source) ||
                    !"MessageProcessed".equals(detailType)) {
                log.warn("Evento ignorado -- source/detail-type no reconocido [source={}]", source);
                return null;
            }

            // -- Extraer el 'detail' del evento -----------------------------
            Map<String, Object> detail = (Map<String, Object>) event.get("detail");

            if (detail == null) {
                log.error("Evento EventBridge sin campo 'detail' -- ignorando");
                return null;
            }

            String messageId      = (String) detail.get("messageId");
            String messageType    = (String) detail.getOrDefault("messageType",   "UNKNOWN");
            String recipientEmail = (String) detail.getOrDefault("recipientEmail","");
            String userEmail      = (String) detail.getOrDefault("userEmail",     "");

            if (messageId == null || messageId.isBlank()) {
                log.error("Evento sin messageId -- no se puede auditar");
                return null;
            }

            log.info("Auditando [messageId={}] [tipo={}] [dest={}]",
                    messageId, messageType, recipientEmail);

            // -- Construir AuditEvent para el caso de uso ------------------
            AuditEvent auditEvent = AuditEvent.builder()
                    .messageId(messageId)
                    .messageType(messageType)
                    .recipientEmail(recipientEmail)
                    .userEmail(userEmail)
                    .source(source)
                    .detailType(detailType)
                    .eventId((String) event.getOrDefault("id", ""))
                    .build();

            // -- Ejecutar caso de uso ----------------------------------------
            // AuditMessageUseCase:
            //   1. Thread.sleep(15_000)
            //   2. DynamoDB UpdateItem PENDING -> COMPLETED (alias #st)
            //   3. SNS Publish
            auditMessagePort.auditMessage(auditEvent, context.getAwsRequestId());

            log.info("AuditHandler completado [messageId={}]", messageId);

        } catch (Exception e) {
            log.error("Error en AuditHandler [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            throw new RuntimeException("Error en AuditHandler: " + e.getMessage(), e);
        }

        return null;
    }
}

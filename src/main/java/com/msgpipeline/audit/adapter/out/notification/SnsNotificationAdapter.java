package com.msgpipeline.audit.adapter.out.notification;

import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * =========================================================================
 * CLASE: SnsNotificationAdapter — Adaptador de Salida (AWS SNS)
 * CAPA: Infraestructura — Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Implementa NotificationPort usando AWS SNS SDK v2.
 *
 * @Profile("aws"): Solo activo en Lambda.
 *
 * TÓPICO SNS: msg-pipeline-email-notifications-sesion-05
 *   Publicar aquí → SNS distribuye a los suscriptores (email).
 *
 * PATRÓN OBSERVER:
 *   El Audit Lambda es el Publisher (this).
 *   Los suscriptores del tópico SNS son los Observers.
 *   Nuevo suscriptor = nueva dirección de email — sin cambiar código.
 *
 * SnsClient: thread-safe, se inicializa estáticamente para warm starts.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class SnsNotificationAdapter implements NotificationPort {

    private static final SnsClient snsClient;

    static {
        snsClient = SnsClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Value("${app.aws.sns-topic-arn:}")
    private String snsTopicArn;

    /**
     * Publica notificación de procesamiento completado en SNS.
     *
     * Si snsTopicArn está vacío, se omite la notificación (compatible
     * con entornos donde no se configuró SNS).
     *
     * FORMATO DEL MENSAJE SNS (email):
     * Asunto: "Mensaje Procesado — msg-pipeline Sesión 05"
     * Cuerpo: datos del mensaje con messageId, tipo y timestamp
     */
    @Override
    public void notificarProcesamiento(AuditEvent auditEvent) {
        if (snsTopicArn == null || snsTopicArn.isBlank()) {
            log.warn("SNS_TOPIC_ARN no configurado — omitiendo notificación [messageId={}]",
                    auditEvent.getMessageId());
            return;
        }

        log.info("Publicando notificación SNS [messageId={}] [topicArn={}]",
                auditEvent.getMessageId(), snsTopicArn);

        // ── Construir el mensaje SNS ──────────────────────────────────────
        String mensaje = String.format("""
                ✅ Mensaje Procesado Exitosamente — msg-pipeline Sesión 05
                
                Detalles del mensaje:
                  ID:          %s
                  Tipo:        %s
                  Destinatario: %s
                  Status:      %s
                  Procesado:   %s
                  EventBridge: %s
                
                Este mensaje fue procesado por el flujo:
                  API Gateway → Processor Lambda → EventBridge → Audit Lambda → SNS
                
                Anku Academy — Especialización Spring Boot + AWS Serverless
                """,
                auditEvent.getMessageId(),
                auditEvent.getMessageType(),
                auditEvent.getRecipientEmail(),
                auditEvent.getFinalStatus(),
                auditEvent.getProcessedAt(),
                auditEvent.getEventId()
        );

        // ── Publicar en SNS ───────────────────────────────────────────────
        PublishRequest request = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject("Mensaje Procesado — msg-pipeline Sesión 05")
                .message(mensaje)
                .build();

        PublishResponse response = snsClient.publish(request);

        log.info("Notificación SNS publicada [messageId={}] [snsMessageId={}]",
                auditEvent.getMessageId(), response.messageId());
    }
}

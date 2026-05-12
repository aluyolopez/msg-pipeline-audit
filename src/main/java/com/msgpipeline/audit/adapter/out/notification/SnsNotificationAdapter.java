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
 * CLASE: SnsNotificationAdapter -- Adaptador de Salida (AWS SNS)
 * CAPA: Infraestructura -- Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("aws"): Solo activo en Lambda.
 * SnsClient: thread-safe, inicializado UNA VEZ en el cold start.
 * PATRON OBSERVER: publica sin conocer los suscriptores de SNS.
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

    @Override
    public void notificarProcesamiento(AuditEvent auditEvent) {
        if (snsTopicArn == null || snsTopicArn.isBlank()) {
            log.warn("SNS_TOPIC_ARN no configurado [messageId={}]", auditEvent.getMessageId());
            return;
        }

        String cuerpo = "Mensaje Auditado y Completado -- msg-pipeline Sesion 07\n\n"
                + "ID:           " + auditEvent.getMessageId()    + "\n"
                + "Tipo:         " + auditEvent.getMessageType()   + "\n"
                + "Destinatario: " + auditEvent.getRecipientEmail()+ "\n"
                + "Status:       " + auditEvent.getFinalStatus()   + "\n"
                + "Procesado:    " + auditEvent.getProcessedAt()   + "\n"
                + "EventBridge:  " + auditEvent.getEventId()       + "\n"
                + "Usuario:      " + auditEvent.getUserEmail()     + "\n\n"
                + "Flujo completado: Cognito JWT -> API Gateway -> Orchestrator"
                + " -> Step Functions -> Validator -> SQS -> Processor"
                + " -> EventBridge -> Audit Lambda\n"
                + "Anku Academy -- Especializacion Spring Boot + AWS Serverless";

        PublishResponse response = snsClient.publish(PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject("Mensaje Auditado -- msg-pipeline Sesion 07")
                .message(cuerpo)
                .build());

        log.info("SNS Publish exitoso [messageId={}] [snsMessageId={}]",
                auditEvent.getMessageId(), response.messageId());
    }
}

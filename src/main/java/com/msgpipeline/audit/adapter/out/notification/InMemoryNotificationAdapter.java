package com.msgpipeline.audit.adapter.out.notification;

import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * CLASE: InMemoryNotificationAdapter — Adaptador de Salida (Memoria)
 * CAPA: Infraestructura — Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Simula la notificación SNS en modo local.
 * @Profile("local"): Solo activo en desarrollo.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryNotificationAdapter implements NotificationPort {

    @Override
    public void notificarProcesamiento(AuditEvent auditEvent) {
        log.info("[LOCAL] Simulando notificación SNS:");
        log.info("[LOCAL]   Tópico:      msg-pipeline-email-notifications-sesion-05 (SIMULADO)");
        log.info("[LOCAL]   messageId:   {}", auditEvent.getMessageId());
        log.info("[LOCAL]   tipo:        {}", auditEvent.getMessageType());
        log.info("[LOCAL]   status:      {}", auditEvent.getFinalStatus());
        log.info("[LOCAL]   processedAt: {}", auditEvent.getProcessedAt());
        log.info("[LOCAL] En AWS real: email enviado al suscriptor del tópico SNS");
    }
}

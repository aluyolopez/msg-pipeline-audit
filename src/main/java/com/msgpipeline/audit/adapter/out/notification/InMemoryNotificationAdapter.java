package com.msgpipeline.audit.adapter.out.notification;

import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
public class InMemoryNotificationAdapter implements NotificationPort {
    @Override
    public void notificarProcesamiento(AuditEvent auditEvent) {
        log.info("[LOCAL] SNS Publish simulado -- Auditoria OK [messageId={}] [status={}]",
                auditEvent.getMessageId(), auditEvent.getFinalStatus());
    }
}

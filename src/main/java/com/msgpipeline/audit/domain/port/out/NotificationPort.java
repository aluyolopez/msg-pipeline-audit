package com.msgpipeline.audit.domain.port.out;

import com.msgpipeline.audit.domain.model.AuditEvent;

/**
 * =========================================================================
 * INTERFAZ: NotificationPort -- Puerto de Salida (SNS Publish)
 * CAPA: Dominio -- Puerto de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * PATRON OBSERVER: El Audit publica sin saber los suscriptores de SNS.
 *
 * IMPLEMENTACIONES:
 *   - SnsNotificationAdapter       --> perfil 'aws'
 *   - InMemoryNotificationAdapter  --> perfil 'local'
 * =========================================================================
 */
public interface NotificationPort {
    void notificarProcesamiento(AuditEvent auditEvent);
}

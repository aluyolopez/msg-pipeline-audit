package com.msgpipeline.audit.domain.port.out;

import com.msgpipeline.audit.domain.model.AuditEvent;

/**
 * =========================================================================
 * INTERFAZ: NotificationPort — Puerto de Salida (SNS)
 * CAPA: Dominio — Puerto de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Puerto de salida para publicar notificaciones de procesamiento completado.
 *
 * PATRÓN: Observer (el Audit Lambda notifica sin saber quiénes escuchan)
 *   El Audit publica en SNS → SNS distribuye a suscriptores:
 *     - Email (suscripción email en SNS)
 *     - SMS (opcional)
 *     - Otras Lambdas (opcional)
 *
 * IMPLEMENTACIONES:
 *   - SnsNotificationAdapter  → perfil 'aws' (SNS real)
 *   - InMemoryNotificationAdapter → perfil 'local' (log en memoria)
 * =========================================================================
 */
public interface NotificationPort {

    /**
     * Publica notificación de procesamiento completado en SNS.
     *
     * En 'aws': SNS.Publish() al tópico msg-pipeline-email-notifications-sesion-05
     * En 'local': loggea la notificación simulada
     *
     * @param auditEvent Evento procesado con datos para el mensaje de notificación
     */
    void notificarProcesamiento(AuditEvent auditEvent);
}

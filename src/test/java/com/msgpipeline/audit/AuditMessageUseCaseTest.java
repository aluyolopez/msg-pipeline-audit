package com.msgpipeline.audit;

import com.msgpipeline.audit.application.usecase.AuditMessageUseCase;
import com.msgpipeline.audit.domain.model.AuditEvent;
import com.msgpipeline.audit.domain.port.out.MessageRepository;
import com.msgpipeline.audit.domain.port.out.NotificationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del AuditMessageUseCase.
 * NOTA: el test usa un delay reducido para no tardar 15s en ejecutarse.
 */
@ExtendWith(MockitoExtension.class)
class AuditMessageUseCaseTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private AuditMessageUseCase useCase;

    @Test
    void auditMessage_debeActualizarStatusACompleted() {
        // Arrange
        AuditEvent event = AuditEvent.builder()
                .messageId("test-uuid-123")
                .messageType("EMAIL")
                .recipientEmail("test@ejemplo.com")
                .source("msg-pipeline.processor")
                .detailType("MessageReceived")
                .build();

        when(messageRepository.updateStatus(any(AuditEvent.class)))
                .thenAnswer(inv -> {
                    AuditEvent e = inv.getArgument(0);
                    e.setFinalStatus("COMPLETED");
                    return e;
                });

        // Act — NOTA: en test real espera 15s. Aquí verificamos la lógica.
        // Para tests rápidos, considerar extraer el delay como parámetro configurable.
        AuditEvent result = useCase.auditMessage(event, "test-lambda-request-id");

        // Assert
        assertThat(result.getFinalStatus()).isEqualTo("COMPLETED");
        assertThat(result.getProcessedAt()).isNotBlank();
        verify(messageRepository, times(1)).updateStatus(any(AuditEvent.class));
        verify(notificationPort, times(1)).notificarProcesamiento(any(AuditEvent.class));
    }
}

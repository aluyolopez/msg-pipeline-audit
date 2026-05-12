package com.msgpipeline.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion del Audit Lambda.
 * VARIABLES DE ENTORNO: DYNAMODB_TABLE_NAME, SNS_TOPIC_ARN
 * TIMEOUT LAMBDA: > 20s (delay 15s + overhead). Recomendado: 60s.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Aws aws = new Aws();
    private Audit audit = new Audit();

    @Data
    public static class Aws {
        private String region = "us-east-1";
        private String dynamodbTable = "msg-pipeline-messages";
        private String snsTopicArn = "";
    }

    @Data
    public static class Audit {
        private int processingDelaySeconds = 15;
        private int ttlDays = 30;
    }
}

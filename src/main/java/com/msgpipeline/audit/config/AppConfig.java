package com.msgpipeline.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración centralizada del Audit Lambda.
 *
 * VARIABLES DE ENTORNO EN LAMBDA:
 *   DYNAMODB_TABLE_NAME = msg-pipeline-messages
 *   SNS_TOPIC_ARN       = arn:aws:sns:us-east-1:{ACCOUNT}:msg-pipeline-email-notifications-sesion-05
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
        /** Delay en segundos antes de actualizar DynamoDB */
        private int processingDelaySeconds = 15;
        private int ttlDays = 30;
    }
}

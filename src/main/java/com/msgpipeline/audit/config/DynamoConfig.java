package com.msgpipeline.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Configuración de DynamoDB para el perfil 'aws'.
 * @Profile("aws"): Solo crea los beans en Lambda.
 */
@Slf4j
@Configuration
@Profile("aws")
public class DynamoConfig {

    @Value("${app.aws.dynamodb-table:msg-pipeline-messages}")
    private String dynamodbTable;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        log.info("Inicializando DynamoDbClient [tabla={}]", dynamodbTable);
        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean
    public String tableName() {
        return dynamodbTable;
    }
}

package com.trecapps.falsehoods.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageServiceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "trecapps.storage", value = "create-strategy", havingValue = "accountAuth")
    IObjectStorageService getAccountStorageService(
            @Value("${trecapps.storage.account-name}") String name,
            @Value("${trecapps.storage.account-key}") String key,
            @Value("${trecapps.storage.blob-endpoint}") String endpoint,
            @Value("${trecapps.storage.container:falsehoods}") String containerName,
            @Value("${trecapps.thumbnail.fallback}") String thumbnailFallback,
            ObjectMapper objectMapperBuilder){
        return new AzureObjectStorageService(name, key, endpoint, containerName, thumbnailFallback, objectMapperBuilder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "trecapps.storage", value = "create-strategy", havingValue = "defaultAuth")
    IObjectStorageService getDefaultStorageService(
            @Value("${trecapps.storage.blob-endpoint}") String endpoint,
            @Value("${trecapps.storage.container:falsehoods}") String containerName,
            @Value("${trecapps.thumbnail.fallback}") String thumbnailFallback,
            ObjectMapper objectMapperBuilder){
        return new AzureObjectStorageService(objectMapperBuilder, thumbnailFallback, containerName, endpoint);
    }
}

package com.trecapps.falsehoods.services;

import com.azure.core.credential.AzureNamedKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trecapps.falsehoods.models.BrandContent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public class AzureObjectStorageService implements IObjectStorageService {

    BlobServiceAsyncClient client;
    ObjectMapper objectMapper;
    BlobContainerAsyncClient containerClient;

    String imageFallback;

    AzureObjectStorageService(String name,
                   String key,
                   String endpoint,
                   String containerName,
                   String imageFallback,
                   ObjectMapper objectMapperBuilder) {
        AzureNamedKeyCredential credential = new AzureNamedKeyCredential(name, key);
        this.client = (new BlobServiceClientBuilder()).credential(credential).endpoint(endpoint).buildAsyncClient();
        this.containerClient = client.getBlobContainerAsyncClient(containerName);
        this.containerClient.createIfNotExists().subscribe();
        this.objectMapper = objectMapperBuilder;
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        this.imageFallback = imageFallback;
    }

    AzureObjectStorageService(
            ObjectMapper objectMapperBuilder,
            String imageFallback,
            String containerName,
            String endpoint
    ) {

        this.client = new BlobServiceClientBuilder()
                .credential(new DefaultAzureCredentialBuilder()
                        .build())
                .endpoint(endpoint)
                .buildAsyncClient();
        this.containerClient = client.getBlobContainerAsyncClient(containerName);
        this.objectMapper = objectMapperBuilder;
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        this.imageFallback = imageFallback;
    }


    @Override
    public Mono<String> retrieveThumbnail(UUID id) {
        BlobAsyncClient blobAsyncClient = this.containerClient.getBlobAsyncClient(String.format("%s-thumbnail", id));

        return blobAsyncClient.exists()
                .flatMap((Boolean exists) -> {
                    if(!exists) return Mono.just(imageFallback);

                    return blobAsyncClient.downloadContent()
                            .map((BinaryData data) -> new String(data.toBytes()));
                });
    }

    @Override
    public Mono<BrandContent> retrieveBrandContent(UUID id) {
        BlobAsyncClient blobAsyncClient = this.containerClient.getBlobAsyncClient(String.format("%s-brand-content.json", id));

        return blobAsyncClient.exists()
                .flatMap((Boolean exists) -> {
                    if(!exists){
                        log.warn("Contents for brand {} not found!", id);
                        return Mono.empty();
                    }
                    return blobAsyncClient.downloadContent()
                            .map((BinaryData data) -> data.toObject(BrandContent.class));
                });
    }
}

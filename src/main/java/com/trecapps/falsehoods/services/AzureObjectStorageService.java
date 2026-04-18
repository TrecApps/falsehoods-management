package com.trecapps.falsehoods.services;

import com.azure.core.credential.AzureNamedKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.serializer.TypeReference;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlockBlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trecapps.falsehoods.models.BrandContent;
import com.trecapps.falsehoods.models.ContentVersion;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

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
                            .flatMap((BinaryData data) -> {
                                byte[] bData = data.toBytes();

                                return blobAsyncClient.getTags()
                                        .map((Map<String,String> tags) -> {
                                           return String.format("data:%s;base64,%s", tags.get("type"),
                                                   Base64.getEncoder().encodeToString(bData));
                                        });

                            });
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

    @Override
    public Mono<BrandContent> saveBrandContent(UUID id, BrandContent content) {

        BlobAsyncClient contentClient = this.containerClient.getBlobAsyncClient(String.format("%s-brand-content.json", id));
        BlobAsyncClient thumbnailClient = this.containerClient.getBlobAsyncClient(String.format("%s-thumbnail", id));

        String imageDataStr = content.getImageData();

        ImageData imageData = imageDataStr == null ? null : this.generateThumbnail(imageDataStr,150, 150);


        return contentClient.upload(BinaryData.fromObject(content))
                .thenReturn(content)
                .flatMap((BrandContent content1) -> {
                    if(imageData == null)
                        return Mono.just(content1);
                    BinaryData data = BinaryData.fromBytes(imageData.thumbnail);
                    BlobHttpHeaders blobHeaders = new BlobHttpHeaders()
                            .setContentType(imageData.imageType);
                    Map<String, String> tags = new HashMap<>();
                    tags.put("type", imageData.imageType);
                    return thumbnailClient.upload(data)
                            .doOnNext((BlockBlobItem item) -> {
                                thumbnailClient.setHttpHeaders(blobHeaders).subscribe();
                                thumbnailClient.setTags(tags);
                            }).thenReturn(content1);
                });
    }

    @Override
    public Mono<SortedSet<ContentVersion>> persistFalsehoodContent(UUID id, String content){
        return getFalsehoodContent(id)
                .flatMap((SortedSet<ContentVersion> pieces) -> {
                    pieces.add(new ContentVersion(pieces.size() + 1, Instant.now(), content));
                    return containerClient.getBlobAsyncClient(getFalsehoodId(id)).upload(BinaryData.fromObject(pieces), true)
                            .thenReturn(pieces);
                });
    }

    @Override
    public Mono<SortedSet<ContentVersion>> getFalsehoodContent(UUID id){
        return Mono.just(getFalsehoodId(id))
                .map((String fileName) -> Optional.ofNullable(this.containerClient.getBlobAsyncClient(fileName)))
                .flatMap((Optional<BlobAsyncClient> blobClient) -> {
                    return blobClient.<Mono<? extends SortedSet<ContentVersion>>>map(blobAsyncClient -> blobAsyncClient.exists().flatMap((Boolean b) -> {
                        if (!b)
                            return Mono.just(new TreeSet<>());

                        return blobAsyncClient.downloadContent().map((BinaryData bd) -> {
                            List<ContentVersion> list = bd.toObject(new TypeReference<>() {
                            });
                            return new TreeSet<>(list);
                        });

                    })).orElseGet(() -> Mono.just(new TreeSet<>()));
                });
    }
}

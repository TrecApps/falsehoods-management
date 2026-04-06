package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.BrandContent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public interface IObjectStorageService {

    Mono<String> retrieveThumbnail(UUID id);

    Mono<BrandContent> retrieveBrandContent(UUID id);


}

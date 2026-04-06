package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class BrandService {

    MongoRepo mongoRepo;
    IObjectStorageService storageService;

    @Autowired
    BrandService(
            MongoRepo mongoRepo1,
            IObjectStorageService storageService1
    ){
        this.mongoRepo = mongoRepo1;
        this.storageService = storageService1;
    }


    public Mono<List<BrandSearchResult>> searchByBrands(boolean hasAccess, int limit, String query, ResourceType resourceType) {
        return this.mongoRepo.findBrands(query, hasAccess, resourceType, limit)
                .flatMap((Brand brand) ->
                    storageService.retrieveThumbnail(brand.getId())
                            .map((String thumbnail) -> {
                                BrandSearchResult result = new BrandSearchResult();
                                result.setBrand(brand);
                                result.setImage(thumbnail);
                                return result;
                            })
                ).collectList();
    }

    public Mono<BrandComplete> retrieveBrand(boolean hasAccess, UUID id){
        return this.mongoRepo.retrieveBrand(id)
                .flatMap((Brand brand) -> {
                    return this.storageService.retrieveBrandContent(brand.getId())
                            .map((BrandContent content) -> {
                                BrandComplete ret = new BrandComplete();
                                ret.setContent(content);
                                ret.setMetadata(brand);
                                return ret;
                            })
                            .switchIfEmpty(Mono.just(new BrandComplete()))
                            .doOnNext((BrandComplete bc) -> {
                                if(bc.getContent() == null)
                                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "");
                            });
                });
    }
}

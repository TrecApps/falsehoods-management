package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.Brand;
import com.trecapps.falsehoods.models.BrandSearchResult;
import com.trecapps.falsehoods.models.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

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

}

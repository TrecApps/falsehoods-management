package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.Record;
import com.trecauth.common.model.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BrandService {

    MongoRepo mongoRepo;
    IObjectStorageService storageService;


    Integer approveThreshold;

    @Autowired
    BrandService(
            MongoRepo mongoRepo1,
            IObjectStorageService storageService1,
            @Value("${trecapps.approve.threshold:1}") Integer approveThreshold1
    ){
        this.mongoRepo = mongoRepo1;
        this.storageService = storageService1;
        this.approveThreshold = approveThreshold1;
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

    public Mono<ResponseObj> persistBrand(BrandComplete complete, UserAccount account){

        Brand metadata = complete.getMetadata();
        if(metadata == null)
            return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "metadata field required in body", null));

        UUID id = metadata.getId();
        Mono<BrandComplete> mono = id == null ? Mono.just(complete) :
                this.mongoRepo.retrieveBrand(id)
                        .flatMap((Brand brand) -> {
                            return this.storageService.retrieveBrandContent(id)
                                    .map((BrandContent content) -> {
                                        BrandComplete complete1 = new BrandComplete();
                                        complete1.setMetadata(brand);
                                        complete1.setContent(content);
                                        return complete1;
                                    })
                                    .doOnNext((BrandComplete complete1) -> {
                                        complete1.getContent().update(complete.getContent());

                                        Brand brand1 = complete1.getMetadata();
                                        brand1.setResourceTypes(metadata.getResourceTypes());
                                        brand1.setNames(metadata.getNames());
                                    });
                        });

        return mono.flatMap((BrandComplete complete1) -> {
            Record record = new Record();
            Brand brand1 = complete1.getMetadata();

            UUID brandId = id;
            if(brandId == null){
                brandId = UUID.randomUUID();
                record.setType("Create");
                brand1.setId(brandId);
            } else {
                record.setType("Edit");
            }

            record.setUCreator(account.getAccountId());
            record.setMade(Instant.now());
            record.setId(UUID.randomUUID());
            record.setResourceId(brandId);


            brand1.getRecords().add(record);

            if(brand1.getReviewStage() == null)
                brand1.setReviewStage(ReviewStage.SUBMITTED);

            UUID finalBrandId = brandId;
            return mongoRepo.saveBrand(brand1)
                    .flatMap((Brand brand2) -> {
                        return this.storageService.saveBrandContent(finalBrandId, complete1.getContent())
                                .thenReturn(complete1);
                    }).thenReturn(ResponseObj.getInstance201("Persisted", brandId.toString()));
        })
                .switchIfEmpty(Mono.just(ResponseObj.getInstance(HttpStatus.NOT_FOUND, "Brand Not found", null)))
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException e) -> Mono.just(e.toResponse()));
    }

    public Mono<ResponseObj> reviewBrand(UserAccount account,
                                         ReviewEntry entry) {
        return mongoRepo.retrieveBrand(entry.getId())
                .flatMap((Brand brand) -> {
                    if(ReviewStage.CONFIRMED.name().equals(brand.getReviewStage()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Brand-Resource Already confirmed");

                    Record record = new Record();
                    record.setUCreator(account.getAccountId());
                    record.setMessages(List.of(entry.getComment()));
                    record.setMade(Instant.now());
                    record.setId(UUID.randomUUID());
                    record.setResourceId(entry.getId());

                    List<Record> records = brand.getRecords();

                    boolean approve = entry.isApprove();

                    for(Record r: records)
                    {
                        if(!account.getId().equals(r.getUCreator()))
                            continue;

                        if("Brand-Reject".equals(r.getType()))
                        {
                            if(approve)
                                r.setType("Brand-Ignore-Reject");
                            else throw new ResponseStatusException(HttpStatus.CONFLICT, "Can't add the same verdict to the same Brand");
                        }

                        if("Brand-Approve".equals(r.getType()))
                        {
                            if(approve)
                                throw new ResponseStatusException(HttpStatus.CONFLICT, "Can't add the same verdict to the same Brand");
                            else
                                r.setType("Brand-Ignore-Approve");
                        }
                    }

                    record.setType(approve ? "Brand-Approve":"Brand-Reject");

                    records.add(record);

                    int approves = 0;
                    int rejects = 0;
                    for(Record r: records)
                    {
                        if("Brand-Approve".equals(r.getType()))
                            approves++;
                        if("Brand-Reject".equals(r.getType()))
                            rejects++;
                    }
                    String returnMessage = "Reviewed";
                    if((approves - rejects) >= approveThreshold)
                    {
                        record = new Record();
                        record.setMade(Instant.now());
                        record.setType("Brand-Confirmed");
                        record.setId(UUID.randomUUID());
                        records.add(record);

                        brand.setReviewStage(ReviewStage.CONFIRMED);
                        returnMessage = "Confirmed";
                    }

                    return this.mongoRepo.saveBrand(brand)
                            .thenReturn(ResponseObj.getInstance200(returnMessage));
                })
                .switchIfEmpty(Mono.just(ResponseObj.getInstance(HttpStatus.NOT_FOUND, "Brand Not found", null)))
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException e) -> Mono.just(e.toResponse()));
    }
}

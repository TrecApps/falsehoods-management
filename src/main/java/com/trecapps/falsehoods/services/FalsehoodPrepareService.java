package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.Record;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class FalsehoodPrepareService {

    @Autowired
    MongoRepo mongoRepo;

    @Autowired
    IObjectStorageService storageService;

    Mono<Boolean> validateBrandExistence(List<UUID> uuids){
        return this.mongoRepo.getBrandsByList(uuids)
                .collectList()
                .map((List<Optional<Brand>> brands) -> {
                    boolean valid = true;
                    for(Optional<Brand> optionalBrand: brands){
                        if(optionalBrand.isEmpty() || !ReviewStage.CONFIRMED.equals(optionalBrand.get().getReviewStage())){
                            valid = false;
                            break;
                        }
                    }
                    return valid;
                });
    }

    String validateTags(String content, List<String> tags){
        if(tags == null)
            return null;

        String cleanContent = content
                .replace("*", "")
                .replace("_", "")
                .replace("  ", " ")
                .toLowerCase(Locale.ROOT);
        for(String tag: tags){
            if(!cleanContent.contains(tag.toLowerCase(Locale.ROOT)))
                return tag;
        }
        return null;
    }

    public Mono<ResponseObj> submitExistingFalsehood(@NotNull AccountList accountList, @NotNull UUID id){
        return mongoRepo.retrieveFalsehood(id)
                .map((Optional<FalsehoodDocument> optionalDoc) -> {

                    if(optionalDoc.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Falsehood not found!");

                    FalsehoodDocument falsehood = optionalDoc.get();

                    if(falsehood.getUCreator().equals(accountList.getMainUserAccount().getId()))
                        // ToDo - this is a breach case, how to handle
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Falsehood does not belong to you!");

                    if(!FalsehoodStage.SAVED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "Falsehood not in the SAVED state!");

                    return falsehood;
                }).flatMap((FalsehoodDocument falsehood) -> {
                    falsehood.setStatus(FalsehoodStage.SUBMITTED);

                    Record record = new Record();
                    record.setId(UUID.randomUUID());
                    record.setResourceId(falsehood.getId());

                    record.setMessages(List.of("Submitted Falsehood"));
                    record.setType("SUBMITTED");
                    record.setUCreator(accountList.getMainUserAccount().getId());
                    record.setCreator(accountList.getMainAccount().getId());

                    record.setMade(Instant.now());

                    falsehood.getRecords().add(record);

                    return mongoRepo.saveFalsehood(falsehood).thenReturn(ResponseObj.getInstance200("Success!"));
                })
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse()))
                .onErrorResume(Throwable.class, (Throwable thrown) -> {
                    log.error("Error Submitting Falsehood {}", id);
                    log.error("Error was ", thrown);
                    return Mono.just(
                            ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Error submitting Falsehood!", null)
                    );
                });
    }

    public Mono<ResponseObj> postFalsehood(@NotNull AccountList accountList, boolean doSubmit, FalsehoodSubmission submission){

        return Mono.just(submission)
                .flatMap((FalsehoodSubmission falsehoodSubmission) -> {

                    if(falsehoodSubmission.getDateMade() == null && !FalsehoodSeverity.TITLE_OR_SLOGAN.equals(submission.getSeverity()))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Date must be provided for non-chronic falsehoods!");
                    List<UUID> brandIds = new ArrayList<>(submission.getCulprits().size() + submission.getTargets().size());
                    brandIds.addAll(submission.getCulprits());
                    brandIds.addAll(submission.getTargets());
                    return validateBrandExistence(brandIds).map((Boolean valid) -> {
                        if(!valid)
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more Brand Ids are not in the Database!");
                        return falsehoodSubmission;
                    });

                }).map((FalsehoodSubmission falsehoodSubmission) -> {
                    FalsehoodFull falsehoodFull = new FalsehoodFull();
                    FalsehoodDocument metadata = falsehoodSubmission.convertToDoc(UUID.randomUUID(), doSubmit);
                    metadata.setUCreator(accountList.getMainUserAccount().getId());
                    metadata.setCreator(accountList.getMainAccount().getId());
                    if(!submission.isShowRegularAccount()){
                        metadata.setAuthorDisplayName(accountList.getMainUserAccount().getDisplayName());
                    } else {
                        metadata.setShowBrand(metadata.getShowBrand());
                        metadata.setAuthorDisplayName(accountList.getMainAccount().getDisplayName());
                    }
                    falsehoodFull.setMetadata(metadata);
                    falsehoodFull.setInitContent(falsehoodSubmission.getContent());
                    return falsehoodFull;
                })
                .flatMap((FalsehoodFull falsehoodFull) ->{
                    FalsehoodDocument metadata = falsehoodFull.getMetadata();
                    return storageService
                            .persistFalsehoodContent(metadata.getId(), falsehoodFull.getInitContent())
                            .flatMap((SortedSet<ContentVersion> cv) -> {

                                Record createRecord = new Record();
                                createRecord.setId(UUID.randomUUID());
                                createRecord.setCreator(metadata.getCreator());
                                createRecord.setUCreator(metadata.getUCreator());
//                                createRecord.set(metadata.getAuthorDisplayName());
                                createRecord.setType("CREATED");
                                createRecord.setMade(Instant.now());

                                metadata.getRecords().add(createRecord);

                                return mongoRepo.saveFalsehood(metadata)//.doOnNext((Falsehood f) -> {
//                                    activityService.submitActivity(createRecord).subscribe();
//                                })
                                        ;

                            }).thenReturn(ResponseObj.getInstance201("Success", metadata.getId().toString()));
                }).onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse()));
    }

    UUID getCreator(FalsehoodDocument falsehood){
        for(Record record : falsehood.getRecords()){
            if("CREATED".equals(record.getType()))
                return record.getUCreator();
        }
        return null;
    }
}

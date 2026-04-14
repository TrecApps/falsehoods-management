package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.Record;
import com.trecauth.common.model.UserAccount;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static com.trecapps.falsehoods.models.RecordEvent.*;
import static com.trecapps.falsehoods.services.FalsehoodAuthorities.EMPLOYEE_AUTH;

@Service
public class FirstReviewService {
    @Autowired
    MongoRepo mongoRepo;

    //@Autowired


    @Value("${trecapps.falsehoods.self-review:false}")
    boolean allowSelfReview;

    @Value("${trecapps.falsehoods.review-threshold:0.66}")
    double threshold;
    @Value("${trecapps.falsehoods.review-threshold:1}")
    int rawThreshold;

    //
    @Value("${trecapps.falsehoods.appeal1-limit:3}")
    int appealLimit;

    void updateCredibility(UserAccount user, int pointChange){
        user.setCredibility(user.getCredibility().add(BigInteger.valueOf(pointChange)));
       // userStorageService.saveUser(user);
    }


    public Mono<ResponseObj> postReview(
            @NotNull AccountList accountList,
            UUID id,
            String comment,
            RecordEvent action){
        return mongoRepo.retrieveFalsehood(id)
                .flatMap((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Falsehood '%s' not found!", id));

                    FalsehoodDocument falsehood = o.get();

                    if(!allowSelfReview && accountList.getMainUserAccount().getId().equals(falsehood.getUCreator()))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own falsehood!");

                    if(FalsehoodStage.R_APPEALED.equals(falsehood.getStatus())){
                        if(!accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH))
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You need to be an employee to review an appealed Falsehood" );
                    } else if(!FalsehoodStage.SUBMITTED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "This Falsehood is not submitted or appealed!");

                    return mongoRepo.findAllRecordsByFalsehoodId(id)
                            .collectList()
                            .map((List<FalsehoodRecord> records) -> {
                                falsehood.getRecords().addAll(records);
                                return falsehood;
                            });
                })

                .flatMap((FalsehoodDocument falsehood) -> {
                    SortedSet<Record> recordList = new TreeSet<>(falsehood.getRecords());
                    List<Record> updateRecords = new LinkedList<>();

                    Record record = new Record();
                    record.setId(UUID.randomUUID());
                    record.setCreator(accountList.getMainAccount().getId());
                    record.setUCreator(accountList.getMainUserAccount().getId());
                    record.setResourceId(id);
                    record.setMade(Instant.now());
                    record.setMessages(List.of(comment));

                    record.setType(action.toString());




                    // Update the needed Records
                    for(Record prevRecord: recordList){
                        if(!accountList.getMainUserAccount().getId().equals(prevRecord.getUCreator())) continue;

                        boolean doUpdate = prevRecord.getId() != null;
                        // If null, then this is part of the original Falsehoods list -  don't update it in the Records table

                        switch(prevRecord.getType()){
                            case "ACCEPT":
                                prevRecord.setType(ACCEPT_OUT.toString());
                                if(doUpdate) updateRecords.add(prevRecord);
                                break;
                            case "REJECT":
                                prevRecord.setType(REJECT_OUT.toString());
                                if(doUpdate) updateRecords.add(prevRecord);
                                break;
                            case "PENALIZE":
                                prevRecord.setType(PENALIZE_OUT.toString());
                                if(doUpdate) updateRecords.add(prevRecord);
                                break;

                        }
                    }

                    // Check if the Falsehood itself is ready to update it's status

                    recordList.add(record);
                    updateRecords.add(record);

                    int accept = 0;
                    int reject = 0;
                    int penalize = 0;
                    for(Record prevRecord: recordList){
                        switch(prevRecord.getType())
                        {
                            case "ACCEPT":
                                accept++;
                                break;
                            case "PENALIZE":
                                penalize++;
                            case "REJECT":
                                reject++;
                        }
                    }


                    if(accept >= rawThreshold && (accept / (double)(accept + reject)) >= threshold){
                        // We have reached the Accept Threshold: go ahead and accept it
                        falsehood.setStatus(FalsehoodStage.ACCEPTED);
                        Record newRecord = new Record();
                        newRecord.setId(UUID.randomUUID());
                        newRecord.setResourceId(id);
                        newRecord.setMade(Instant.now().plusNanos(10));
                        newRecord.setType("ACCEPTED");
                        updateRecords.add(newRecord);

                        updateCredibility(accountList.getMainUserAccount(), 5);
                    } else if( reject >= rawThreshold && (reject / (double)(accept + reject)) >= threshold){
                        falsehood.setStatus(FalsehoodStage.REJECTED);
                        Record newRecord = new Record();
                        newRecord.setId(UUID.randomUUID());
                        newRecord.setResourceId(id);
                        newRecord.setMade(Instant.now().plusNanos(10));
                        newRecord.setType("REJECTED");
                        updateRecords.add(newRecord);

                        if(penalize/(double)reject > 0.5)
                            updateCredibility(accountList.getMainUserAccount(), 15);
                    }

                    return mongoRepo.saveRecords(updateRecords).collectList().thenReturn(falsehood)
//                            .doOnNext((FalsehoodDocument f) -> {
//                                activityService.submitActivity(record).subscribe();
//                            })
                            ;

                })
                .flatMap((FalsehoodDocument f) -> {
//                    if(action.equals(SUGGEST) || f.getStatus().equals(FalsehoodStage.REJECTED) || f.getStatus().equals(FalsehoodStage.ACCEPTED)){
//                        //notify(f, action);
//                    }

                    f.setRecords(
                            f.getRecords().stream().filter((Record r) -> r.getId() == null).sorted().toList()
                    );

                    return mongoRepo.saveFalsehood(f).thenReturn(ResponseObj.getInstance200("Success!"));
                });

    }

    public Mono<ResponseObj> appeal(
            @NotNull AccountList accountList,
            UUID id,
            String comment){
        return mongoRepo.retrieveFalsehood(id)
                .flatMap((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Falsehood '%s' not found!", id));

                    FalsehoodDocument falsehood = o.get();

                    if(!FalsehoodStage.REJECTED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Falsehood is not in the rejected Stage!");

                    if(!accountList.getMainUserAccount().getId().equals(falsehood.getUCreator()))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner may appeal a rejected Falsehood!");
                    if(appealLimit >= 0){
                        return mongoRepo.findAllRecordsByFalsehoodId(falsehood.getId()).collectList()
                                .map((List<FalsehoodRecord> records) ->{
                                    records.addAll(falsehood.getRecords().stream().map(FalsehoodRecord::getInstance).toList());
                                    int appeals = 0;
                                    for(Record prevRecord: records){
                                        if(RecordEvent.APPEAL_1.toString().equals(prevRecord.getType()))
                                            appeals++;
                                    }

                                    if(appeals >= appealLimit)
                                        throw new ResponseStatusException(
                                                HttpStatus.PRECONDITION_FAILED,
                                                String.format("Appeal count for this Falsehood has reach the limit of %d", appealLimit));
                                    return falsehood;
                                });
                    }
                    return Mono.just(falsehood);
                })
                .flatMap((FalsehoodDocument falsehood) -> {
                    if(!FalsehoodStage.REJECTED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Falsehood is not in the rejected Stage!");

                    if(!accountList.getMainUserAccount().getId().equals(falsehood.getUCreator()))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner may appeal a rejected Falsehood!");
                    if(appealLimit >= 0){
                        return mongoRepo.findAllRecordsByFalsehoodId(falsehood.getId()).collectList()
                                .map((List<FalsehoodRecord> records) ->{
                                    records.addAll(falsehood.getRecords().stream().map(FalsehoodRecord::getInstance).toList());
                                    int appeals = 0;
                                    for(Record prevRecord: records){
                                        if(RecordEvent.APPEAL_1.toString().equals(prevRecord.getType()))
                                            appeals++;
                                    }

                                    if(appeals >= appealLimit)
                                        throw new ResponseStatusException(
                                                HttpStatus.PRECONDITION_FAILED,
                                                String.format("Appeal count for this Falsehood has reach the limit of %d", appealLimit));
                                    return falsehood;
                                });
                    }
                    return Mono.just(falsehood);

                })
                .flatMap((FalsehoodDocument falsehood) -> {
                    falsehood.setStatus(FalsehoodStage.R_APPEALED);

                    Record newRecord = new Record();
                    newRecord.setId(UUID.randomUUID());
                    newRecord.setCreator(accountList.getMainAccount().getId());
                    newRecord.setUCreator(accountList.getMainUserAccount().getId());
                    newRecord.setMade(Instant.now());
                    newRecord.setType(APPEAL_1.toString());
                    newRecord.setMessages(List.of(comment));
                    falsehood.getRecords().add(newRecord);
                    return mongoRepo.saveFalsehood(falsehood)
//                            .doOnNext((FalsehoodDocument f) -> {
//                                activityService.submitActivity(newRecord).subscribe();
//                            })
                            .thenReturn(ResponseObj.getInstance200("Success"));
                });
    }
}

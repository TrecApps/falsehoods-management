package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.Account;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.Record;
import com.trecauth.common.model.UserAccount;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

import static com.trecapps.falsehoods.models.RecordEvent.*;
import static com.trecapps.falsehoods.services.FalsehoodAuthorities.EMPLOYEE_AUTH;
import static com.trecapps.falsehoods.services.FalsehoodAuthorities.FALSEHOOD_JUR;

@Service
public class SecondReviewService {

    @Value("${trecapps.falsehoods.review-threshold:0.66}")
    double threshold;
    @Value("${trecapps.falsehoods.review-threshold-raw:1}")
    int rawThreshold;
    @Value("${trecapps.falsehoods.appeal2-limit:3}")
    int appealLimit;
    @Value("${trecapps.falsehoods.confirm-points:15}")
    int confirmPoints;

    @Value("${trecapps.falsehoods.max-deny-points:50}")
    int maxDenyPoints;

    @Value("${trecapps.falsehoods.max-deny-points:1050}")
    int jurorPointThreshold;

    @Value("${trecapps.falsehoods.self-review:false}")
    boolean allowSelfReview;

    @Autowired
    MongoRepo mongoRepo;

    void updateCredibility(UserAccount user, int pointChange){
        user.setCredibility(user.getCredibility().add(BigInteger.valueOf(pointChange)));
        // userStorageService.saveUser(user);
    }

//    int deniedPoints(FalsehoodDocument falsehood, List<Record> toUpdate){
//        int ret = 0;
//        for(Record prevRecord:falsehood.getRecords()){
//            if("DENIED".equals(prevRecord.getType())){
//                Integer taken = prevRecord.getPoints();
//                if(taken != null && taken > 0){
//                    ret += taken;
//                    prevRecord.setPoints(-taken);
//                    if(prevRecord.getId() != null){
//                        toUpdate.add(prevRecord);
//                    }
//                }
//            }
//        }
//        return ret;
//    }


    public Mono<ResponseObj> postReview(
            @NotNull AccountList accountList,
            UUID id,
            String comment,
            RecordEvent action,
            int removePoints
    ) {
        return mongoRepo.retrieveFalsehood(id)
                .flatMap((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Falsehood '%s' not found!", id));

                    FalsehoodDocument falsehood = o.get();

                    if(!allowSelfReview && accountList.getMainUserAccount().getId().equals(falsehood.getUCreator()))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own falsehood!");

                    if(!FalsehoodStage.S_APPEALED.equals(falsehood.getStatus()) && !FalsehoodStage.ACCEPTED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Falsehood needs to be in the appealed or Accepted Stage");

                    List<String> authRoles = accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

                    if(!authRoles.contains(EMPLOYEE_AUTH) && FalsehoodStage.S_APPEALED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Appeals Falsehoods need to be reviewed by an Employee");

                    if(!authRoles.contains(EMPLOYEE_AUTH) && !authRoles.contains(FALSEHOOD_JUR))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You need to be an employee or have the Juror Badge to make a second level Falsehood");


                    // ToDo - check the first time Falsehood was in the Accepted state and make sure enough time has passed

                    // End ToDo

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
                    record.setUCreator(accountList.getMainUserAccount().getId());
                    record.setCreator(accountList.getMainAccount().getId());
                    record.setResourceId(id);
                    record.setMade(Instant.now().plusNanos(10));
                    record.setMessages(List.of(comment));

                    record.setType(action.toString());

//                    if (action == DENY) {
//                        if (removePoints > 0)
//                            record.setPoints(Math.min(removePoints,maxDenyPoints));
//                    }


                    // Update the needed Records
                    for (Record prevRecord : recordList) {
                        if (!accountList.getMainUserAccount().getId().equals(prevRecord.getUCreator())) continue;

                        boolean doUpdate = prevRecord.getId() != null;
                        // If null, then this is part of the original Falsehoods list -  don't update it in the Records table

                        switch (prevRecord.getType()) {
                            case "CONFIRM":
                                prevRecord.setType(CONFIRM_OUT.toString());
                                if (doUpdate) updateRecords.add(prevRecord);
                                break;
                            case "DENY":
                                prevRecord.setType(DENY_OUT.toString());
                                if (doUpdate) updateRecords.add(prevRecord);
                                break;

                        }
                    }

                    // Check if the Falsehood itself is ready to update it's status

                    recordList.add(record);
                    updateRecords.add(record);

                    int accept = 0;
                    int reject = 0;
                    int penalize = 0;
                    int penalizeCount = 0;
                    for (Record prevRecord : recordList) {
                        switch (prevRecord.getType()) {
                            case "CONFIRM":
                                accept++;
                                break;
                            case "DENY": {
                                reject++;
//                                Integer deductPoints = prevRecord.getPoints();
//                                if (deductPoints != null && deductPoints > 0) {
//                                    penalize += deductPoints;
//                                    penalizeCount++;
//                                }
                            }
                        }
                    }

                    if (accept >= rawThreshold && (accept / (double) (accept + reject)) >= threshold) {
                        // We have reached the Accept Threshold: go ahead and accept it
                        falsehood.setStatus(FalsehoodStage.CONFIRMED);
                        Record newRecord = new Record();
                        newRecord.setId(UUID.randomUUID());
                        newRecord.setResourceId(id);
                        newRecord.setMade(Instant.now().plusNanos(10));
                        newRecord.setType(CONFIRMED.toString());
                        updateRecords.add(newRecord);

                        // Check to see if person previously lost points from a denial
                        //int addPoints = confirmPoints + deniedPoints(falsehood, updateRecords);

                        //updateCredibility(user, addPoints);
                    } else if (reject >= rawThreshold && (reject / (double) (accept + reject)) >= threshold) {
                        falsehood.setStatus(FalsehoodStage.DENIED);
                        Record newRecord = new Record();
                        newRecord.setId(UUID.randomUUID());
                        newRecord.setResourceId(id);
                        newRecord.setMade(Instant.now().plusNanos(10));
                        newRecord.setType(DENIED.toString());
                        updateRecords.add(newRecord);

                        if (penalizeCount > 0){
                            int take = (int) ((double) penalize / penalizeCount);
                            updateCredibility(accountList.getMainUserAccount(), -take);
                            //newRecord.setPoints(take);
                        }
                    }

                    return mongoRepo.saveRecords(updateRecords)
//                            .doOnNext((Record f) -> {
//                                activityService.submitActivity(record).subscribe();
//                            })
                            .collectList().thenReturn(falsehood);
                })
                .flatMap((FalsehoodDocument f) -> {
                    if(action.equals(SUGGEST) || f.getStatus().equals(FalsehoodStage.REJECTED) || f.getStatus().equals(FalsehoodStage.ACCEPTED)){
                        //notify(f, action);
                    }

                    f.setRecords(
                            f.getRecords().stream().filter((Record r) -> r.getId() == null).sorted().toList()
                    );

                    return mongoRepo.saveFalsehood(f).thenReturn(ResponseObj.getInstance200("Success!"));
                });

    }

//    boolean isBrandLinked(Account brands, FalsehoodDocument falsehood){
//        String brandId = brands == null ? null : brands.ge();
//        return brandId != null && falsehood.getCulprits().contains(brandId);
//    }

    public Mono<ResponseObj> appeal(
            @NotNull AccountList accountList,
            UUID id,
            String comment){
        return mongoRepo.retrieveFalsehood(id)
                .flatMap((Optional<FalsehoodDocument> o) -> {

                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Falsehood '%s' not found!", id));

                    FalsehoodDocument falsehood = o.get();

                    if(!FalsehoodStage.CONFIRMED.equals(falsehood.getStatus()) && !FalsehoodStage.DENIED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Falsehood is not in the rejected Stage!");

                    UserAccount user = accountList.getMainUserAccount();

                    if(!(
                            user.getCredibility().compareTo(BigInteger.valueOf(jurorPointThreshold)) > -1 || !user.getId().equals(falsehood.getUCreator()) ||
                                    accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH)
                                    //|| isBrandLinked(brands, falsehood)
                    ))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                String.format(
                                    "Only the submitter, an Employee, someone named in the Falsehood, or one with Juror credibility ( > %d) may make this appeal!",
                                        this.jurorPointThreshold
                        ));


                    if(appealLimit >= 0){
                        return mongoRepo.findAllRecordsByFalsehoodId(falsehood.getId()).collectList()
                                .map((List<FalsehoodRecord> records) ->{
                                    records.addAll(falsehood.getRecords().stream().map(FalsehoodRecord::getInstance).toList());
                                    int appeals = 0;
                                    for(Record prevRecord: records){
                                        if(RecordEvent.APPEAL_2.toString().equals(prevRecord.getType()))
                                            appeals++;
                                    }

                                    if(appeals >= appealLimit)
                                        throw new ResponseStatusException(
                                                HttpStatus.PRECONDITION_FAILED,
                                                String.format("Appeal count for this Falsehood has reach the limit of %d", appealLimit)
                                                );
                                    return falsehood;
                                });
                    }
                    return Mono.just(falsehood);

                })
                .flatMap((FalsehoodDocument falsehood) -> {
                    falsehood.setStatus(FalsehoodStage.S_APPEALED);

                    Record newRecord = new Record();
                    newRecord.setId(UUID.randomUUID());
                    newRecord.setCreator(accountList.getMainAccount().getId());
                    newRecord.setUCreator(accountList.getMainUserAccount().getId());
                    newRecord.setMade(Instant.now());
                    newRecord.setType(APPEAL_2.toString());
                    newRecord.setMessages(List.of(comment));
                    falsehood.getRecords().add(newRecord);
                    return mongoRepo.saveFalsehood(falsehood)
//                            .doOnNext((Falsehood f) -> {
//                        activityService.submitActivity(newRecord).subscribe();
//                    })
                            .thenReturn(ResponseObj.getInstance200("Success"));
                });
    }
}

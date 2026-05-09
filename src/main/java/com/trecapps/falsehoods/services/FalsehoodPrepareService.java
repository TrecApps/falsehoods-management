package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.Record;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static com.trecapps.falsehoods.services.FalsehoodAuthorities.EMPLOYEE_AUTH;

@Service
@Slf4j
public class FalsehoodPrepareService {

    @Autowired
    MongoRepo mongoRepo;

    @Autowired
    IObjectStorageService storageService;

    private static final List<String> employeePatchFields = List.of(
            "culprits",
            "culprits-remove",
            "severity",
            "targets",
            "targets-remove"
    );

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

    boolean attemptSetBrowserDate(FalsehoodDocument falsehood, String date){
        String[] datePieces = date.split("-");
        if(datePieces.length != 3)
            return false;

        try{
            int year = Integer.parseInt(datePieces[0]);
            int month = Integer.parseInt(datePieces[1]);
            int day = Integer.parseInt(datePieces[2]);

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            falsehood.setDateMade(cal.getTime());
            return true;
        } catch(Exception e){
            log.error("Could not parse {}", date, e);
            return false;
        }
    }

    public Mono<ResponseObj> submitExistingFalsehood(@NotNull AccountList accountList, @NotNull UUID id){
        return mongoRepo.retrieveFalsehood(id)
                .map((Optional<FalsehoodDocument> optionalDoc) -> {

                    if(optionalDoc.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Falsehood not found!");

                    FalsehoodDocument falsehood = optionalDoc.get();

                    if(!falsehood.getUCreator().equals(accountList.getMainUserAccount().getId()))
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

    public Mono<ResponseObj> patchFalsehood(@NotNull AccountList accountList, UUID falsehoodId, FalsehoodPatch patch){

        return Mono.just(falsehoodId)
                .flatMap(mongoRepo::retrieveFalsehood)
                .map((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Falsehood Entry Not Found");

                    FalsehoodDocument falsehood = o.get();

                    boolean acceptState = false;
                    if(FalsehoodStage.SAVED.equals(falsehood.getStatus()) ||
                            FalsehoodStage.SUBMITTED.equals(falsehood.getStatus())){
                        acceptState = true;
                        if(!accountList.getMainUserAccount().getId().equals(getCreator(falsehood)))
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator of the falsehood may alter the falsehood at the SAVED/SUBMITTED Stage!");
                    }

                    if(FalsehoodStage.ACCEPTED.equals(falsehood.getStatus())){
                        acceptState = true;
                        if(!accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH))
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only TrecApps Falsehood staff may alter a falsehood at the ACCEPTED Stage!");

                        if(!employeePatchFields.contains(patch.getField()))
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employees may only change the following fields: " + employeePatchFields);

                    }

                    if(!acceptState)
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Falsehoods can only be altered at the SAVED, SUBMITTED, or ACCEPTED Stage!");

                    return falsehood;
                })
                .flatMap((FalsehoodDocument falsehood) -> {

                    final String[] value = {patch.getValue()};

                    return storageService.getFalsehoodContent(falsehoodId)
                            .flatMap((SortedSet<ContentVersion> contents) -> {
                                boolean removeOp = true;
                                switch(patch.getField()){
                                    case "culprits":
                                    case "targets":
                                        removeOp = false;
                                    case "culprits-remove":
                                    case "targets-remove":

                                    {
                                        UUID brandId;
                                        try{
                                            brandId = UUID.fromString(value[0]);
                                        } catch(Exception ignore){
                                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value needs to be in valid UUID format");
                                        }

                                        boolean finalRemoveOp = removeOp;
                                        return this.mongoRepo.retrieveBrand(brandId)
                                                .map(Optional::of)
                                                .switchIfEmpty(Mono.just(Optional.empty()))
                                                .flatMap((Optional<Brand> brand) -> {
                                                    if(brand.isEmpty())
                                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand not currently listed in the database!");

                                                    List<UUID> list = patch.getField().startsWith("targets")
                                                            ? falsehood.getTargets() : falsehood.getCulprits();

                                                    if(finalRemoveOp)
                                                        list.remove(brandId);
                                                    else
                                                        list.add(brandId);

                                                    return this.mongoRepo.saveFalsehood(falsehood);
                                                });
                                    }

                                    case "title":
                                        if(value[0] == null || value[0].trim().isEmpty())
                                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be blank!");
                                        // ToDo: Verify the contents of the title


                                        // End ToDo

                                        falsehood.setTitle(value[0].trim());
                                        break;
                                    case "severity":
                                    {
                                        try{
                                            FalsehoodSeverity severity = FalsehoodSeverity.valueOf(value[0]);
                                            falsehood.setSeverity(severity);
                                        } catch(IllegalArgumentException exception){
                                            throw new ResponseStatusException( HttpStatus.BAD_REQUEST, String.format("Argument of '%s' cannot be applied to 'severity'", value[0]));
                                        }
                                        break;
                                    }
                                    case "dateMade":
                                    {
                                        try{
                                            if(value[0] == null)
                                                falsehood.setDateMade(null);
                                            else {
                                                String[] fields = value[0].split("-");
                                                if(fields.length >=2){
                                                    LocalDate newDate = LocalDate.of(
                                                            Integer.parseInt(fields[0]),
                                                            Integer.parseInt(fields[1]),
                                                            Integer.parseInt(fields[2])
                                                    );
                                                    falsehood.setDateMade(
                                                            Date.from(newDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                                                    );
                                                }
                                            }
                                        }catch(NumberFormatException exception){
                                            if(!attemptSetBrowserDate(falsehood, value[0]))
                                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Argument of '%s' cannot be applied to a date", value[0]));
                                        }
                                        break;
                                    }
                                    case "notes":
                                        falsehood.setNotes(value[0]);
                                    case "content":{

//                                        List<String> tags = falsehood.getTags().stream().toList();
//                                        String tag = validateTags(value[0], tags);
//                                        if(tag != null)
//                                            throw new ObjectResponseException(String.format("Tag '%s' not found in the new body!", tag), HttpStatus.BAD_REQUEST);

                                        return storageService.persistFalsehoodContent(falsehoodId, value[0]).thenReturn(falsehood);
                                    }
                                    default:
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Unknown field '%s'", patch.getField()));



//                                    if(falsehoodSubmission.getDateMade() == null && !FalsehoodSeverity.TITLE_OR_SLOGAN.equals(submission.getSeverity()))
//                                        throw new ObjectResponseException("A Date must be provided for non-chronic falsehoods!", HttpStatus.BAD_REQUEST);
                                }

                                return Mono.just(falsehood);
                            });

                }).flatMap((FalsehoodDocument falsehood) -> {

                    // ToDo - Once a Records Service is set up, Have it update the
                    //  records not yet added to the Falsehood Document


                    // End ToDo


                    if(!FalsehoodStage.ACCEPTED.equals(falsehood.getStatus())) {
                        for (Record record : falsehood.getRecords()) {
                            if ("ACCEPT".equals(record.getType()))
                                record.setType("ACCEPT_OUT");
                            else if("REJECT".equals(record.getType()))
                                record.setType("REJECT_OUT");
                        }
                    }

                    return mongoRepo.saveFalsehood(falsehood)
                            .thenReturn(ResponseObj.getInstance200("Success!"));

                }).onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse()));

    }


    public Mono<ResponseObj> deleteFalsehood(@NotNull AccountList accountList, UUID falsehoodId) {
        return Mono.just(falsehoodId)
                .flatMap(mongoRepo::retrieveFalsehood)
                .map((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Falsehood Entry Not Found");

                    FalsehoodDocument falsehood = o.get();

                    if(FalsehoodStage.DELETED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Object already 'deleted'");

                    boolean userAccept = false;
                    if(accountList.getMainUserAccount().getId().equals(getCreator(falsehood))){
                        userAccept = true;

                        if(FalsehoodStage.CONFIRMED.equals(falsehood.getStatus()))
                            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Cannot delete a 'confirmed' falsehood!");

                        if(FalsehoodStage.R_APPEALED.equals(falsehood.getStatus()) ||
                                FalsehoodStage.S_APPEALED.equals(falsehood.getStatus()))
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "Falsehood is currently under appeal!");
                    }

                    else if(accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH)){
                        userAccept = true;
                    }

                    if(!userAccept)
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this falsehood entry");
                    return falsehood;
                })

                .flatMap((FalsehoodDocument f) -> {
                    f.setStatus(FalsehoodStage.DELETED);

                    Record deleteRecord = new Record();
                    deleteRecord.setId(UUID.randomUUID());
                    deleteRecord.setMade(Instant.now());
                    deleteRecord.setUCreator(accountList.getMainUserAccount().getId());
                    deleteRecord.setResourceId(f.getId());
                    deleteRecord.setCreator(accountList.getMainAccount().getId());
                    deleteRecord.setType("DELETED");
                    f.getRecords().add(deleteRecord);
                    return mongoRepo.saveFalsehood(f)
                            .thenReturn(ResponseObj.getInstance200("Success"));


                }).onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse()));

    }
}

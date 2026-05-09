package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.AccountList;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.trecapps.falsehoods.services.FalsehoodAuthorities.EMPLOYEE_AUTH;
import static com.trecapps.falsehoods.services.FalsehoodAuthorities.FALSEHOOD_SUB;

@Service
public class BriefService {


    @Value("${trecapps.falsehoods.brief-cred:55}")
    int briefCredibility;

    @Autowired
    MongoRepo mongoRepo;

    boolean canLeaveBrief(FalsehoodDocument falsehood, AccountList accountList){
        UserAccount user = accountList.getMainUserAccount();
        if(user.getId().equals(falsehood.getUCreator())
                || user.getCredibility().compareTo(BigInteger.valueOf(briefCredibility)) <= 0
                || accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH))
            return true;

        return false; //isBrandLinked(brands, falsehood);
    }

    public boolean canLeaveBrief(FalsehoodRet falsehood, AccountList accountList){
        UserAccount user = accountList.getMainUserAccount();
        if(user.getId().equals(falsehood.getUCreator())
                || user.getCredibility().compareTo(BigInteger.valueOf(briefCredibility)) <= 0
                || accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH))
            return true;

        return false; //isBrandLinked(brands, falsehood);
    }

//    boolean isBrandLinked(TcBrands brands, Falsehood falsehood){
//        String brandId = brands == null ? null : brands.getInfoId();
//        return brandId != null && (
//                brandId.equals(falsehood.getInstitution()) ||
//                        brandId.equals(falsehood.getMediaOutlet()) ||
//                        brandId.equals(falsehood.getPublicFigure())
//        );
//    }


    public Mono<ResponseObj> postBrief(@NotNull AccountList accountList, UUID falsehoodId, String content, String type){
        return mongoRepo.retrieveFalsehood(falsehoodId)
                .flatMap((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Falsehood '%s' not found!", falsehoodId));

                    FalsehoodDocument falsehood = o.get();

                    if(!FalsehoodStage.ACCEPTED.equals(falsehood.getStatus()))
                        throw new ResponseStatusException(
                                HttpStatus.PRECONDITION_FAILED,
                                String.format("Falsehoods need to be in the ACCEPTED state before having briefs submitted! This one is in the %s state", falsehood.getStatus())
                        );

                    if(!canLeaveBrief(falsehood, accountList))
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                String.format("Only the submitter, an employee, a brand Account linked to the Falsehood, or someone with a credibility of %d can leave a brief!", this.briefCredibility)

                        );

                    BriefPurpose purpose = switch(type.toLowerCase(Locale.ROOT)){
                        case "support", "affirm" -> BriefPurpose.AFFIRM;
                        case "oppose" -> BriefPurpose.OPPOSE;
                        case "neutral", "suggest" -> BriefPurpose.SUGGEST;
                        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brief type must be 'support', 'oppose', or 'neutral'!");
                    };

                    Brief newBrief = new Brief();
                    newBrief.setId(UUID.randomUUID());
                    newBrief.setPurpose(purpose);
                    newBrief.setCreated(Instant.now());
                    newBrief.setAccount(accountList.getMainAccount().getId());
                    newBrief.setUAccount(accountList.getMainUserAccount().getId());
                    newBrief.update(content);
                    newBrief.setFalsehoodId(falsehood.getId());

                    return mongoRepo.saveBrief(newBrief)
                            //.flatMap((Brief b) -> {
                            //    return activityService.submitActivity(b).thenReturn(b);
                            //})
                            .doOnNext((Brief brief) -> {
                                // ToDo - notify submitter that a brief has been added
                            })
                            .map((Brief brief) -> ResponseObj.getInstance201("Successfully Added!", brief.getId().toString()));

                });
    }


    public Mono<ResponseObj> editBrief(AccountList accountList, UUID falsehoodId, UUID briefId, String newContent){
        return mongoRepo.retrieveFalsehood(falsehoodId)
                .flatMap((Optional<FalsehoodDocument> o) -> {
                    if(o.isEmpty())
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Falsehood does not exist!");

                    return mongoRepo.retrieveBriefById(briefId);
                })
                .flatMap((Brief brief) -> {
                    if(brief == null)
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brief does not exist!");

                    if(!brief.getUAccount().equals(accountList.getMainUserAccount().getId()))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You did not make this brief! If you used a Brand Account, you need to use that Brand Account to edit");

                    List<String> authRoles = accountList.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

//                    if(!authRoles.contains(FALSEHOOD_SUB) && !authRoles.contains(EMPLOYEE_AUTH))
//                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You need to be an employee or have the '(Subscription name here)' active");

                    brief.update(newContent);

                    return mongoRepo.saveBrief(brief).thenReturn(ResponseObj.getInstance200("Success"));
                });
    }
}

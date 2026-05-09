package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.RecordEvent;
import com.trecapps.falsehoods.models.ResponseObj;
import com.trecapps.falsehoods.models.SecondReviewSubmission;
import com.trecapps.falsehoods.services.FirstReviewService;
import com.trecapps.falsehoods.services.ResponseStatusException;
import com.trecapps.falsehoods.services.SecondReviewService;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.TrecauthAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.UUID;

import static com.trecapps.falsehoods.services.FalsehoodAuthorities.EMPLOYEE_AUTH;

@RestController
@RequestMapping("/falsehood-review-api")
@Slf4j
public class FirstReviewController {

    @Autowired
    FirstReviewService firstReviewService;

    @Autowired
    SecondReviewService secondReviewService;

    @Value("${trecapps.falsehoods.review1-cred:45}")
    int credibility;


    @PostMapping(value = "/{action:approve|reject|penalize|suggest}")
    Mono<ResponseEntity<ResponseObj>> postFirstReview(Authentication authentication,
                                                 @PathVariable String action,
                                                 @RequestBody SecondReviewSubmission submission
                                                 ){
        AccountList list = ((TrecauthAuthentication)authentication).getList();
        return Mono.just(list)
                .flatMap((AccountList user) -> {
                    if(user.getMainUserAccount().getCredibility().compareTo(BigInteger.valueOf(credibility)) < 0&&
                            !user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(EMPLOYEE_AUTH))
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                String.format(
                                        "You credibility of %d falls short of the %d needed to make this call!",
                                        user.getMainUserAccount().getCredibility(), credibility) );
                    RecordEvent recordEvent;
                    switch(action){

                        case "approve":
                            recordEvent = RecordEvent.ACCEPT; break;
                        case "reject":
                            recordEvent = RecordEvent.REJECT; break;
                        case "penalize":
                            recordEvent = RecordEvent.PENALIZE; break;
                        case "suggest":
                            recordEvent = RecordEvent.SUGGEST; break;
                        default: throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"This should not happen!" );
                    }

                    return firstReviewService.postReview(user,submission.getFalsehoodId(), submission.getComment(), recordEvent);
                }).map(ResponseObj::toEntity)
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse().toEntity()))
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Submitting Level 1 Review!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Review Falsehood", null).toEntity());
                });
    }

    @PostMapping(value = "/appeal-1/{id}",
            consumes = MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<ResponseObj>> appealRejection(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody String comment
    ){
        AccountList list = ((TrecauthAuthentication)authentication).getList();
        return firstReviewService.appeal(list, id, comment).map(ResponseObj::toEntity)
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse().toEntity()))
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Appealing Rejection!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Appeal Rejection", null).toEntity());
                });
    }

    @PostMapping(value = "/{action:confirm|deny}")
    Mono<ResponseEntity<ResponseObj>> postReview(
            Authentication authentication,
            @PathVariable String action,
            @RequestBody SecondReviewSubmission body
    ) {
        Integer points = body.getDenyPoints();
        AccountList list = ((TrecauthAuthentication)authentication).getList();
        return secondReviewService.postReview(
                        list,
                        body.getFalsehoodId(),
                        body.getComment(),
                        "confirm".equals(action) ? RecordEvent.CONFIRM : RecordEvent.DENY,
                        points == null ? 0 : points)
                .map(ResponseObj::toEntity)
                .onErrorResume(ResponseStatusException.class,
                        (ResponseStatusException ex) -> Mono.just(ex.toResponse().toEntity()))
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Posting Second Level Review!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Review Accepted Falsehood", null).toEntity());
                });

    }

    @PostMapping(value = "/appeal-2/{id}",
            consumes = MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<ResponseObj>> appeal(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody String comment
    ) {
        AccountList list = ((TrecauthAuthentication)authentication).getList();
        return secondReviewService.appeal(list, id, comment)
                .map(ResponseObj::toEntity)
                .onErrorResume(ResponseStatusException.class,
                        (ResponseStatusException ex) -> Mono.just(ex.toResponse().toEntity()))
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Appealing Second Review!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Submit appeal", null).toEntity());
                });

    }

}

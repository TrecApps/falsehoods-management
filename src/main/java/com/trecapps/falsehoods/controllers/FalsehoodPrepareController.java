package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.FalsehoodPatch;
import com.trecapps.falsehoods.models.FalsehoodSubmission;
import com.trecapps.falsehoods.models.ResponseObj;
import com.trecapps.falsehoods.services.FalsehoodPrepareService;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.TrecauthAuthentication;
import com.trecauth.common.model.UserAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.UUID;

@RestController
@RequestMapping("/Falsehood-api")
@Slf4j
public class FalsehoodPrepareController {

    @Autowired
    FalsehoodPrepareService falsehoodPrepareService;

    @PostMapping
    Mono<ResponseEntity<ResponseObj>> postFalsehood(
            @RequestParam(defaultValue = "false") Boolean doSubmit,
            @RequestBody FalsehoodSubmission submission,
            Authentication auth
            )
    {
        AccountList list = ((TrecauthAuthentication)auth).getList();
        UserAccount user = list.getMainUserAccount();
        if(user.getCredibility().compareTo(BigInteger.valueOf(5)) < 0)
            return Mono.just(
                    new ResponseEntity<>(
                            ResponseObj.getInstance(HttpStatus.FORBIDDEN, "Your credibility rating is under 5", null),
                            HttpStatus.FORBIDDEN));

        return falsehoodPrepareService.postFalsehood(list, doSubmit, submission)
                .map(ResponseObj::toEntity)
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Posting Falsehood!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Post Falsehood", null).toEntity());
                });

    }

    @GetMapping("/submit/{id}")
    Mono<ResponseEntity<ResponseObj>> submitExistingFalsehood(
            @PathVariable UUID id,
            Authentication auth
    ) {
        AccountList list = ((TrecauthAuthentication)auth).getList();
        return falsehoodPrepareService.submitExistingFalsehood(list, id)
                .map(ResponseObj::toEntity);
    }

    @PatchMapping("/{id}")
    Mono<ResponseEntity<ResponseObj>> changeFalsehood(
            @PathVariable UUID id,
            @RequestBody FalsehoodPatch patch,
            Authentication auth
            ){
        AccountList list = ((TrecauthAuthentication)auth).getList();
        return falsehoodPrepareService.patchFalsehood(list, id, patch)
                .map(ResponseObj::toEntity)
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Updating Falsehood!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Update Falsehood", null).toEntity());
                });
    }

    @DeleteMapping("/{id}")
    Mono<ResponseEntity<ResponseObj>> deleteFalsehood(
            @PathVariable UUID id,
            Authentication auth
    ){
        AccountList list = ((TrecauthAuthentication)auth).getList();
        return falsehoodPrepareService.deleteFalsehood(list, id)
                .map(ResponseObj::toEntity)
                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Deleting Falsehood!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to soft delete Falsehood", null).toEntity());
                });
    }


}

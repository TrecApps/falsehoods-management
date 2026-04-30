package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.ResponseObj;
import com.trecapps.falsehoods.services.BriefService;
import com.trecapps.falsehoods.services.ResponseStatusException;
import com.trecauth.common.model.TrecauthAuthentication;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/Brief")
@Slf4j
public class BriefController {

    @Data
    public static class BriefSubmission{
        String type;
        String content;
    }

    @Autowired
    BriefService briefService;

    @PostMapping("/{id}")
    Mono<ResponseEntity<ResponseObj>> postBrief(
            Authentication authentication,
            @RequestBody BriefSubmission submission,
            @PathVariable UUID id
    ) {
        TrecauthAuthentication trecauthAuthentication = (TrecauthAuthentication) authentication;
        return briefService.postBrief(trecauthAuthentication.getList(), id, submission.getContent(), submission.getType())
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse()))
                .map(ResponseObj::toEntity)

                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Posting Brief!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Post Brief", null).toEntity());
                });
    }

    @PutMapping(value = "/{f_id}/{id}", consumes= MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<ResponseObj>> editBrief(
            Authentication authentication,
            @RequestBody String content,
            @PathVariable UUID f_id,
            @PathVariable UUID id
    ){
        TrecauthAuthentication trecauthAuthentication = (TrecauthAuthentication) authentication;
        return briefService.editBrief(trecauthAuthentication.getList(), f_id, id, content)
                .onErrorResume(ResponseStatusException.class, (ResponseStatusException ex) -> Mono.just(ex.toResponse()))
                .map(ResponseObj::toEntity)

                .onErrorResume((Throwable thrown) -> {
                    log.error("Error Updating Brief!", thrown);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Update Brief", null).toEntity());
                });
    }


}

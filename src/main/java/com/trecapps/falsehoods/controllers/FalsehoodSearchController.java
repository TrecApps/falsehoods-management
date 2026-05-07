package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.FalsehoodRet;
import com.trecapps.falsehoods.models.FalsehoodSearch;
import com.trecapps.falsehoods.models.FalsehoodStage;
import com.trecapps.falsehoods.services.FalsehoodSearchService;
import com.trecauth.common.model.TrecauthAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RequestMapping("/falsehood-search-api")
@RestController
public class FalsehoodSearchController {

    @Autowired
    FalsehoodSearchService searchService;

    @PostMapping
    Mono<ResponseEntity<List<FalsehoodRet>>> doSearch(
            @RequestBody FalsehoodSearch search,
            @RequestParam(defaultValue = "CONFIRMED") FalsehoodStage status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            ){

        return searchService.searchFalsehoods(search, status, page, size)
                .map(ResponseEntity::ok)
                ;

    }

    @GetMapping
    Mono<ResponseEntity<List<FalsehoodRet>>> doSearch(
            Authentication authentication,
            @RequestParam(required = false) Optional<FalsehoodStage> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        TrecauthAuthentication trecauthAuthentication = (TrecauthAuthentication) authentication;
        return searchService.searchFalsehoods(
                trecauthAuthentication.getList().getMainUserAccount().getId(),
                status.orElse(null),
                page,
                size)
                .map(ResponseEntity::ok);
    }


}

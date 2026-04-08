package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.BrandComplete;
import com.trecapps.falsehoods.models.ResponseObj;
import com.trecapps.falsehoods.models.ReviewEntry;
import com.trecapps.falsehoods.services.BrandService;
import com.trecauth.common.model.TrecauthAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/brands-update-api")
public class BrandUpdateController {

    @Autowired
    BrandService brandService;


    @PostMapping
    Mono<ResponseEntity<ResponseObj>> addBrand(
            @RequestBody BrandComplete complete,
            Authentication authentication
    ){
        TrecauthAuthentication trecauthAuthentication = (TrecauthAuthentication) authentication;

        return brandService.persistBrand(complete, trecauthAuthentication.getList().getMainUserAccount())
                .map(ResponseObj::toEntity);
    }

    @PutMapping
    Mono<ResponseEntity<ResponseObj>> reviewBrand(
            @RequestBody ReviewEntry entry,
            Authentication authentication){
        TrecauthAuthentication trecauthAuthentication = (TrecauthAuthentication) authentication;

        return brandService.reviewBrand(trecauthAuthentication.getList().getMainUserAccount(), entry)
                .map(ResponseObj::toEntity);
    }
}

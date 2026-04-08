package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.BrandComplete;
import com.trecapps.falsehoods.models.BrandSearchResult;
import com.trecapps.falsehoods.models.ResourceType;
import com.trecapps.falsehoods.services.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/brands-api")
public class BrandSearchController {

    @Autowired
    BrandService brandService;

    @GetMapping
    Mono<ResponseEntity<List<BrandSearchResult>>> getBrandsByQuery(
            @RequestParam String query,
            @RequestParam(required = false)ResourceType resourceType,
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean seekAll
            ){
        final int tempLimit = limit;
        return ReactiveSecurityContextHolder.getContext()
                .flatMap((SecurityContext context) -> {
                    boolean hasAccess = false;
                    // ToDo - read the context to determine if requester can create/edit brands


                    // End ToDo

                    int cLimit = tempLimit;

                    if(cLimit > 10) cLimit = 10;
                    if(cLimit < 1) cLimit = 1;

                    return brandService.searchByBrands(
                            seekAll && hasAccess,
                            cLimit,
                            query,
                            resourceType
                            );
                }).map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    Mono<ResponseEntity<BrandComplete>> getBrand(
            @PathVariable UUID id
            ){
        return ReactiveSecurityContextHolder.getContext()
                .flatMap((SecurityContext context) -> {
                    boolean hasAccess = false;
                    // ToDo - read the context to determine if requester can create/edit brands


                    // End ToDo
                    return brandService.retrieveBrand(hasAccess, id);

                }).map(ResponseEntity::ok);
    }
}

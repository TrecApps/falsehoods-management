package com.trecapps.falsehoods.controllers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Component
public class LoginRouter {

    @Value("${trecapps.login.url}")
    String loginUrl;
    @Value("${trecapps.falsehoods.url}")
    String falsehoodsUrl;
    @Value("${trecapps.falsehoods.base-path}")
    String falsehoodsPath;



    public Mono<ServerResponse> loginPage(ServerRequest request){
        Map<String, Object> dataMap = new HashMap<>();

        dataMap.put("userServiceUrl", loginUrl);
        dataMap.put("gatewayPath", falsehoodsUrl);
        dataMap.put("baseUrl", falsehoodsPath);

        String rawTarget;
        try {
            rawTarget = request.queryParam("target")
                    .map(t -> URLDecoder.decode(t, StandardCharsets.UTF_8))
                    .orElse("");
        } catch (IllegalArgumentException e) {
            rawTarget = "";
        }
        if (rawTarget.length() > 2048) {
            rawTarget = rawTarget.substring(0, 2048);
        }

        dataMap.put("redirectTarget", rawTarget);

        return ServerResponse.ok().render("Login", dataMap);
    }

}

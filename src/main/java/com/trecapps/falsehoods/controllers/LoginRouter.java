package com.trecapps.falsehoods.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoginRouter {

    @Value("${trecapps.login.url}")
    String loginUrl;
    @Value("${trecapps.falsehoods.url}")
    String falsehoodsUrl;



    public Mono<ServerResponse> loginPage(ServerRequest request){
        Map<String, Object> dataMap = new HashMap<>();

        dataMap.put("userServiceUrl", loginUrl);
        dataMap.put("gatewayPath", falsehoodsUrl);

        return ServerResponse.ok().render("Login", dataMap);
    }

}

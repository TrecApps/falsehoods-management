package com.trecapps.falsehoods.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.trecapps.falsehoods.services.WelcomeService;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.TrecauthAuthentication;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FrontendRouter {

    @Autowired
    WelcomeService welcomeService;

    @Data
    static
    class FrontendData<T> {
        AccountList accountList;
        T Data;
    }

    <T> Mono<FrontendData<T>> prepareData(){
        return ReactiveSecurityContextHolder.getContext()
                .map((SecurityContext context) -> {
                    FrontendData<T> data = new FrontendData<>();
                    if(context.getAuthentication() instanceof TrecauthAuthentication trecAuthentication){
                        data.setAccountList(trecAuthentication.getList());
                    }
                    return data;
                });
    }

    public Mono<ServerResponse> welcomePage(ServerRequest request) {
        Mono<FrontendData<List<WelcomeService.Guideline>>> thData = this.prepareData();
        thData = thData.doOnNext((FrontendData<List<WelcomeService.Guideline>> frontendData) -> {
            frontendData.setData(welcomeService.getGuidelines());
        });

        return thData.flatMap((FrontendData<List<WelcomeService.Guideline>> data) -> {

            String elementStyle = "element-container-default";

//            String profilePic = user == null ? null :
//                    brands == null ? "User-" + user.getId() : "Brand-" + brands.getId();

            Map<String, Object> dataMap = new HashMap<>();
//            dataMap.put("user", data.getUser());
//            dataMap.put("brand", data.getBrand());
//            dataMap.put("profilePic", String.format("%s/%s", this.imageUrl, profilePic));
            dataMap.put("guidelines", data.getData());
            dataMap.put("elementContainerSetting", elementStyle);
            dataMap.put("elementItemSetting", elementStyle.replace("container", "item"));

            return ServerResponse.ok().render("Welcome", dataMap);

        });
    }
}

package com.trecapps.falsehoods.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trecapps.falsehoods.models.Brand;
import com.trecapps.falsehoods.models.BrandComplete;
import com.trecapps.falsehoods.models.BrandContent;
import com.trecapps.falsehoods.models.ReviewStage;
import com.trecapps.falsehoods.services.BrandService;
import com.trecapps.falsehoods.services.WelcomeService;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.TrecauthAuthentication;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FrontendRouter {

    @Autowired
    WelcomeService welcomeService;

    @Autowired
    BrandService brandService;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${trecapps.image.url}")
    String imageUrl;
    @Value("${trecapps.falsehoods.url}")
    String falsehoodsUrl;
    @Value("${trecapps.falsehoods.base-path}")
    String falsehoodsPath;



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

    String getPathVariable(String name, ServerRequest request){
        try{
            return request.pathVariable(name);
        } catch(IllegalArgumentException ignore){
            return null;
        }
    }

    void prepStyles(Map<String, Object> dataMap, AccountList list){
        String elementStyle = "element-container-default";

        // ToDo - get details of styles

        // End ToDo

        dataMap.put("elementContainerSetting", elementStyle);
        dataMap.put("elementItemSetting", elementStyle.replace("container", "item"));
    }

    String convertObjectToString(Object o){
        try{
            return objectMapper.writeValueAsString(o);
        } catch(JsonProcessingException e){
            return null;
        }
    }

    @SneakyThrows
    Map<String, Object> getDataMap(FrontendData<?> data){
        Map<String, Object> dataMap = new HashMap<>();
        AccountList list = data.getAccountList();
        dataMap.put("list", list);
        dataMap.put("imageServiceUrl", imageUrl);
        dataMap.put("falsehoodServiceUrl", falsehoodsUrl);
        dataMap.put("baseUrl", falsehoodsPath);
        dataMap.put("isResourceEmployee", list != null && list.getMainAccount().getPermissions().contains("RESOURCE_EMPLOYEE"));
        if(list != null){
            String profilePic = String.format("%s/profile/%s", this.imageUrl, list.getMainAccount().getId());
            dataMap.put("profilePic", profilePic);
        }

        prepStyles(dataMap, list);
        return dataMap;
    }

    private BrandComplete generateBlankBrandComplete(){
        BrandComplete ret = new BrandComplete();

        Brand brand = new Brand();
        brand.setDefaultLanguage("en-us");
//        brand.setReviewStage(ReviewStage.PRE_SUBMIT);
        ret.setMetadata(brand);

        BrandContent content = new BrandContent();
        content.setContent("");
        content.setImageDescription("");
        content.setMetadata(new HashMap<>());
        ret.setContent(content);

        return ret;
    }

    public Mono<ServerResponse> articleEditPage(ServerRequest request){
        Mono<FrontendData<BrandComplete>> thData = this.prepareData();

        thData = thData.flatMap((FrontendData<BrandComplete> frontendData) -> {
            String id = getPathVariable("id", request);

            Mono<BrandComplete> completeMono;

            if(id == null){
                completeMono = Mono.just(generateBlankBrandComplete());
            } else {
                completeMono = this.brandService.retrieveBrand(true, UUID.fromString(id));
            }

            return completeMono.map((BrandComplete bc) -> {
                frontendData.setData(bc);
                return frontendData;
            });
        });


        return thData.flatMap((FrontendData<BrandComplete> data) -> {
            Map<String, Object> dataMap = getDataMap(data);

            BrandComplete complete = data.getData();

            dataMap.put("brandContent", complete.getContent());
            dataMap.put("metadata", complete.getMetadata());

            return ServerResponse.ok().render("ArticleEdit", dataMap);
        });



    }

    public Mono<ServerResponse> articlePage(ServerRequest request) {
        String id = getPathVariable("id", request);
        if(id == null){
            return ServerResponse.badRequest().bodyValue("'id' is a required path variable!");

        }
        Mono<FrontendData<BrandComplete>> thData = this.prepareData();
        thData = thData.flatMap((FrontendData<BrandComplete> frontendData) -> {
            boolean hasAccess = false;
            // ToDo - determine if requester has access

            //
            return brandService.retrieveBrand(hasAccess, UUID.fromString(id))
                    .map((BrandComplete complete) -> {
                        frontendData.setData(complete);
                        return frontendData;
                    });
        });

        return thData.flatMap((FrontendData<BrandComplete> data) -> {
            Map<String, Object> dataMap = getDataMap(data);

            BrandComplete complete = data.getData();

            boolean isResourceEmployee = false;
            if(data.getAccountList() != null){
                AccountList list = data.getAccountList();
                isResourceEmployee = list
                        .getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
                        .contains("RESOURCE_EMPLOYEE");
            }
            // ToDo - determine if requester has access

            dataMap.put("reviewStage", complete.getMetadata().getReviewStage().toString());
            dataMap.put("isResourceEmployee", isResourceEmployee);
            dataMap.put("brandPic", complete.getContent().getImageData());
            dataMap.put("imgDesc", complete.getContent().getImageDescription());
            dataMap.put("entries", complete.getContent().getMetaDataAsEntries());
            dataMap.put("brandContent", complete.getContent().getContent());
            dataMap.put("brandId", id);
            dataMap.put("brandName", complete.getMetadata().getNames().getFirst());

            return ServerResponse.ok().render("Article", dataMap);
        });
    }

    public Mono<ServerResponse> welcomePage(ServerRequest request) {
        Mono<FrontendData<List<WelcomeService.Guideline>>> thData = this.prepareData();
        thData = thData.doOnNext((FrontendData<List<WelcomeService.Guideline>> frontendData) -> {
            frontendData.setData(welcomeService.getGuidelines());
        });

        return thData.flatMap((FrontendData<List<WelcomeService.Guideline>> data) -> {

            Map<String, Object> dataMap = getDataMap(data);

            dataMap.put("guidelines", data.getData());

            return ServerResponse.ok().render("Welcome", dataMap);

        });
    }
}

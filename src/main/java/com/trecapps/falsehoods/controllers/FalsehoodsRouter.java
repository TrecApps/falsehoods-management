package com.trecapps.falsehoods.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trecapps.falsehoods.models.*;
import com.trecapps.falsehoods.services.BriefService;
import com.trecapps.falsehoods.services.FalsehoodSearchService;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.TrecauthAuthentication;
import com.trecauth.common.model.UserAccount;
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

import java.math.BigInteger;
import java.util.*;

@Component
public class FalsehoodsRouter extends BaseRouter{

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FalsehoodSearchService falsehoodSearchService;

    @Value("${trecapps.image.url}")
    String imageUrl;
    @Value("${trecapps.falsehoods.url}")
    String falsehoodsUrl;
    @Value("${trecapps.falsehoods.base-path}")
    String falsehoodsPath;
    @Value("${trecapps.login.url}")
    String loginUrl;

    @Autowired
    BriefService briefService;

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

    @SneakyThrows
    Map<String, Object> getDataMap(FrontendData<?> data){
        Map<String, Object> dataMap = new HashMap<>();
        AccountList list = data.getAccountList();
        UserAccount userAccount = list == null ? null : list.getMainUserAccount();
        dataMap.put("list", list);
        dataMap.put("imageServiceUrl", imageUrl);
        dataMap.put("falsehoodServiceUrl", falsehoodsUrl);
        dataMap.put("userServiceUrl", loginUrl);
        dataMap.put("baseUrl", falsehoodsPath);
        dataMap.put("credibility", userAccount == null ? BigInteger.ZERO : userAccount.getCredibility());
        //dataMap.put("isResourceEmployee", list != null && list.getMainAccount().getPermissions().contains("RESOURCE_EMPLOYEE"));
        if(list != null){
            String profilePic = String.format("%s/profile/%s", this.imageUrl, list.getMainAccount().getId());
            dataMap.put("profilePic", profilePic);
        }

        prepStyles(dataMap, list);
        return dataMap;
    }

    public Mono<ServerResponse> falsehoodSearchPage(ServerRequest request){
        Mono<FrontendData<Object>> thData = this.prepareData();

        return thData.flatMap((FrontendData<Object> data) -> {
            Map<String, Object> dataMap = getDataMap(data);
            return ServerResponse.ok().render("falsehood-search", dataMap);
        });
    }

    public Mono<ServerResponse> falsehoodSubmitPage(ServerRequest request){
        Mono<FrontendData<Object>> thData = this.prepareData();

        return thData.flatMap((FrontendData<Object> data) -> {
            Map<String, Object> dataMap = getDataMap(data);
            return ServerResponse.ok().render("falsehood-submit", dataMap);
        });
    }

    private boolean canReview(FalsehoodFull currentFalsehood, AccountList list){
        if( list == null) return false;
        UserAccount user = list.getMainUserAccount();
        if(user == null) return false;
        FalsehoodStage status = currentFalsehood.getFullMetaData().getStatus();
        List<String> authRoles = list.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if(status == FalsehoodStage.SUBMITTED) {
            return (user.getCredibility().compareTo(BigInteger.valueOf(45)) > 0 || authRoles.contains("FALSEHOOD_EMPLOYEE"));
        }
        if(status == FalsehoodStage.ACCEPTED) {
            return authRoles.contains("FALSEHOOD_EMPLOYEE") || authRoles.contains("FALSEHOOD_JUR");
        }
        if(status == FalsehoodStage.R_APPEALED){
            return authRoles.contains("FALSEHOOD_EMPLOYEE");
        }
        return false;
    }

    private boolean canAppeal(FalsehoodFull currentFalsehood, AccountList list){
        if( list == null) return false;
        UserAccount user = list.getMainUserAccount();
        if(user == null) return false;
        FalsehoodStage status = currentFalsehood.getFullMetaData().getStatus();
        List<String> authRoles = list.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        if(status == FalsehoodStage.REJECTED){
            return currentFalsehood.getFullMetaData().getUCreator().equals(user.getId());
        }

        if(status == FalsehoodStage.CONFIRMED || status == FalsehoodStage.DENIED) {

            // ToDo - Support Brand Accounts linked to Brands
//            let brand = this.authService.tcBrand?.infoId;
//            if(brand && (brand == meta.publicFigure?.brandId || brand == meta.mediaOutlet?.brandId ||brand == meta.institution?.brandId ))
//            return true;

            return currentFalsehood.getFullMetaData().getUCreator().equals(user.getId()) ||
                    authRoles.contains("FALSEHOOD_JUR") ||
                    authRoles.contains("FALSEHOOD_EMPLOYEE");
        }

        return false;
    }

    public Mono<ServerResponse> falsehoodPage(ServerRequest request){
        Mono<FrontendData<FalsehoodFull>> thData = this.prepareData();

        thData = thData.flatMap((FrontendData<FalsehoodFull> frontendData) -> {
            String id = getPathVariable("id", request);

            UUID userId = null;
            AccountList list = frontendData.getAccountList();
            if(list != null){
                UserAccount userAccount = list.getMainUserAccount();
                userId = userAccount == null ? null: userAccount.getId();
            }

            Mono<FalsehoodFull> completeMono =
                    this.falsehoodSearchService.getFalsehood(userId, UUID.fromString(id));

            return completeMono.map((FalsehoodFull bc) -> {
                frontendData.setData(bc);
                return frontendData;
            });
        });

        return thData.flatMap((FrontendData<FalsehoodFull> data) -> {
            Map<String, Object> dataMap = getDataMap(data);

            FalsehoodFull complete = data.getData();
            AccountList list = data.getAccountList();
            UserAccount user = list == null ? null : list.getMainUserAccount();

            FalsehoodRet fRet = complete.getFullMetaData();
            dataMap.put("canSubEdit", user != null &&
                    fRet.getUCreator().equals(user.getId()) &&
                    (FalsehoodStage.SAVED.equals(fRet.getStatus()) || FalsehoodStage.SUBMITTED.equals(fRet.getStatus())));
            dataMap.put("canEmpEdit", list != null &&
                    list.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains("FALSEHOOD_EMPLOYEE") &&
                    FalsehoodStage.ACCEPTED.equals(fRet.getStatus()));
            dataMap.put("falsehood", complete);

            dataMap.put("canSubmit", user != null &&
                    fRet.getUCreator().equals(user.getId()) &&
                    FalsehoodStage.SAVED.equals(fRet.getStatus()));
            dataMap.put("canReview", canReview(complete, list));
            dataMap.put("canAppeal", canAppeal(complete, list));

            dataMap.put("falsehoodContent", complete.getContent().getLast().contents());
            dataMap.put("isAccepted", fRet.getStatus() == FalsehoodStage.ACCEPTED);
            dataMap.put("canAddBrief", list != null && briefService.canLeaveBrief(fRet, list));
            dataMap.put("blankString", "");
            List<String> reviewOptions = new ArrayList<>();
            if(fRet.getStatus() == FalsehoodStage.SUBMITTED){
                reviewOptions.add("approve");
                reviewOptions.add("reject");
                reviewOptions.add("penalize");
                reviewOptions.add("suggest");
            } else if(fRet.getStatus() == FalsehoodStage.ACCEPTED){
                reviewOptions.add("confirm");
                reviewOptions.add("deny");
            }
            dataMap.put("reviewOptions", reviewOptions);


            return ServerResponse.ok().render("falsehood", dataMap);
        });

    }
}

package com.trecapps.falsehoods.controllers;

import com.trecapps.falsehoods.models.FrontendData;
import com.trecauth.common.model.AccountList;
import com.trecauth.common.model.TrecauthAuthentication;
import com.trecauth.common.model.UserAccount;
import lombok.Data;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Map;

public class BaseRouter {

    @Data
    static class StyleSpec {

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

    String getElementStyle(String defaultStyle, Object style){
        try{
            Class<?> styleClass = style.getClass();
            Field field = styleClass.getField("useDark");
            field.setAccessible(true);
            boolean useDark = field.getBoolean(style);
            field = styleClass.getField("style");
            String styleStr = field.get(style).toString();
            String ret = "element-container";
            if(useDark)
                ret = ret + "-dark";
            return ret + "-" + styleStr;
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            return defaultStyle;
        }
    }

    void prepStyles(Map<String, Object> dataMap, AccountList list){
        String elementStyle = "element-container-default";

        // ToDo - get details of styles
        UserAccount user = list == null ? null : list.getMainUserAccount();
        if(user != null){
            Object stylesObject = user.getExtensions().get("styles");

            if(stylesObject instanceof Map<?,?> stylesMap){
                Object style = stylesMap.get("Falsehoods-Service");
                elementStyle = getElementStyle(elementStyle, style);
            }
        }

        // End ToDo

        dataMap.put("elementContainerSetting", elementStyle);
        dataMap.put("elementItemSetting", elementStyle.replace("container", "item"));
    }




}

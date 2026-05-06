package com.trecapps.falsehoods.models;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public class ResponseObj {
    int status;
    String message;
    String id;
    transient HttpStatus httpStatus;

    public ResponseEntity<ResponseObj> toEntity(){
        return new ResponseEntity<>(this, this.httpStatus);
    }

    public static ResponseObj getInstance(HttpStatus status, String message, String id){
        ResponseObj obj = new ResponseObj();
        obj.httpStatus = status;
        obj.status = status.value();
        obj.message = message;
        obj.id = id;
        return obj;
    }

    public static ResponseObj getInstance200(String message){
        return getInstance(HttpStatus.OK, message, null);
    }

    public static ResponseObj getInstance404(String message){
        return getInstance(HttpStatus.NOT_FOUND, message, null);
    }

    public static ResponseObj getInstance400(String message){
        return getInstance(HttpStatus.BAD_REQUEST, message, null);
    }

    public static ResponseObj getInstance201(String message, String id){
        return getInstance(HttpStatus.CREATED, message, id);
    }
}

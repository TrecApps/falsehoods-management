package com.trecapps.falsehoods.services;


import com.trecapps.falsehoods.models.ResponseObj;
import org.springframework.http.HttpStatus;

public class ResponseStatusException extends RuntimeException {
    HttpStatus status;

    public ResponseStatusException(HttpStatus status, String message){
        super(message);
        this.status = status;
    }

    public ResponseObj toResponse(){
        return ResponseObj.getInstance(status, this.getMessage(), null);
    }
}

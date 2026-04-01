package com.trecapps.falsehoods.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrandService {

    MongoRepo mongoRepo;

    @Autowired
    BrandService(
            MongoRepo mongoRepo1
    ){
        this.mongoRepo = mongoRepo1;
    }


    
}

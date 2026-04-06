package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.Brand;
import com.trecapps.falsehoods.models.ResourceType;
import com.trecapps.falsehoods.models.ReviewStage;
import jakarta.annotation.Nullable;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MongoRepo {

    @Getter
    ReactiveMongoTemplate template;
    @Getter
    String brandsCollection;
    @Getter
    String falsehoodsCollection;
    @Getter
    String falsehoodRecordsCollection;
    @Getter
    String briefsCollection;

    @Autowired
    MongoRepo(
            @Qualifier("trecappsFalsehoodsMongoTemplate") ReactiveMongoTemplate template,
            @Value("${trecapps.mongo.brands.collection:brands}") String brandsCollection1,
            @Value("${trecapps.mongo.falsehoods.collection:falsehoods}") String falsehoodsCollection1,
            @Value("${trecapps.mongo.records.collection:brands}") String falsehoodRecordsCollection1,
            @Value("${trecapps.mongo.briefs.collection:image-profiles}") String briefsCollection1
    ){
        this.template = template;
        this.brandsCollection = brandsCollection1;
        this.briefsCollection = briefsCollection1;
        this.falsehoodRecordsCollection = falsehoodRecordsCollection1;
        this.falsehoodsCollection = falsehoodsCollection1;
    }


    Flux<Brand> findBrands(String search, boolean all, @Nullable ResourceType resourceType, int limit){

        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(
                Criteria.where("names").is(search)
        );
        if(!all)
            criteriaList.add(
                    Criteria.where("reviewStage").is(ReviewStage.CONFIRMED)
            );

        if(resourceType != null)
            criteriaList.add(
                    Criteria.where("resourceTypes").is(resourceType)
            );

        MatchOperation matchOperation = new MatchOperation(new Criteria().andOperator(criteriaList));
        LimitOperation limitOperation = new LimitOperation(limit);

        Aggregation aggregation = Aggregation.newAggregation(matchOperation, limitOperation);

        return this.template.aggregate(aggregation, this.brandsCollection, Brand.class);
    }

    Mono<Brand> retrieveBrand(UUID uuid){
        return this.template.findById(uuid, Brand.class, this.brandsCollection);
    }
}

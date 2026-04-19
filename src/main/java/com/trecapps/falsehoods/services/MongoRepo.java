package com.trecapps.falsehoods.services;

import com.trecapps.falsehoods.models.*;
import com.trecauth.common.model.Record;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

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

    Flux<Optional<Brand>> getBrandsByList(Collection<UUID> uuids) {
        Query query = new Query().addCriteria(new Criteria().where("id_").in(uuids));

        return this.template.find(query, Brand.class, this.brandsCollection)
                .map(Optional::of)
                .switchIfEmpty(Mono.just(Optional.empty()));
    }

    Flux<Brand> findBrands(String search, boolean all, @Nullable ResourceType resourceType, int limit){

        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(
                Criteria.where("names").regex(search, "i")
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

    Mono<Brand> saveBrand(Brand brand){
        return this.template.save(brand, this.brandsCollection);
    }

    Mono<Optional<FalsehoodDocument>> retrieveFalsehood(UUID id){
        return this.template.findById(id, FalsehoodDocument.class, this.falsehoodsCollection)
                .map(Optional::of)
                .switchIfEmpty(Mono.just(Optional.empty()));
    }

    Mono<FalsehoodDocument> saveFalsehood(FalsehoodDocument document){
        return this.template.save(document, this.falsehoodsCollection);
    }

    Flux<FalsehoodDocument> searchFalsehoods(Aggregation aggregation){
        return this.template.aggregate(aggregation, this.falsehoodsCollection, FalsehoodDocument.class);
    }

    Flux<FalsehoodRecord> findAllRecordsByFalsehoodId(UUID id){
        Query query = new Query().addCriteria(Criteria.where("f_resource_id").is(id));
        return this.template.find(query, FalsehoodRecord.class, this.falsehoodRecordsCollection);
    }

    Flux<FalsehoodRecord> saveRecords(Collection<Record> records){
        return this.template.insertAll(Mono.just(records).map((Collection<Record> r) -> {
            return r
                    .stream()
                    .map(FalsehoodRecord::getInstance)
                    .toList();
        }), this.falsehoodRecordsCollection);
    }

    Flux<Brief> retrieveBriefsByFalsehood(UUID falsehoodId){
        Query query = new Query().addCriteria(Criteria.where("falsehoodId").is(falsehoodId));
        return this.template.find(query, Brief.class, this.briefsCollection);
    }

    Mono<Brief> retrieveBriefById(UUID id){
        return this.template.findById(id, Brief.class, this.briefsCollection);
    }

    Mono<Brief> saveBrief(Brief brief){
        return this.template.save(brief, this.briefsCollection);
    }

}

package com.trecapps.falsehoods.services;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;

@Configuration
@ConditionalOnProperty(prefix = "trecapps.falsehoods.repo", name = "strategy", havingValue = "mongo-uri")
public class MongoImageUrlConfiguration {

    @Bean
    public MongoClient trecappsFalsehoodsMongoClient(
            @Value("${trecapps.mongo.falsehoods.uri}")String mongoUri
    ) {

        return MongoClients.create(MongoClientSettings.builder().uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(new ConnectionString(mongoUri)).build());
    }

    @Bean
    @Primary
    public ReactiveMongoDatabaseFactory trecappsFalsehoodsMongoFactory(
            @Qualifier("trecappsFalsehoodsMongoClient") MongoClient client,
            @Value("${trecapps.mongo.falsehoods.database}") String database
    ){
        return new SimpleReactiveMongoDatabaseFactory(client, database);
    }

    @Bean
    public ReactiveMongoTemplate trecappsFalsehoodsMongoTemplate(
            @Qualifier("trecappsFalsehoodsMongoFactory") ReactiveMongoDatabaseFactory factory
    ) {
        return new ReactiveMongoTemplate(factory);
    }

}

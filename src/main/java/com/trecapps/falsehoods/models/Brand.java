package com.trecapps.falsehoods.models;

import com.trecauth.common.model.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an entity to which Falsehoods can be attributed
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Brand extends Resource {

    /**
     * The ID of the Brand
     */
    @MongoId
    UUID id;

    /**
     * Names the Brand is known as
     */
    List<String> names = new ArrayList<>();

    /**
     * The Type of Brand (institution, public figure, etc.)
     */
    List<ResourceType> resourceTypes = new ArrayList<>();

    /**
     * The language the original article was written in
     */
    String defaultLanguage;

    /**
     * Verification status (can it be used?)
     */
    ReviewStage reviewStage;
    /**
     * Link to a verified Brand Account
     */
    UUID brandId;
}

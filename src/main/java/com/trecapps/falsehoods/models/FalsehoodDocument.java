package com.trecapps.falsehoods.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trecauth.common.model.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * The main Falsehood Class to persist into the Mongo Database
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FalsehoodDocument extends Resource {

    /**
     * The main ID of the Falsehood
     */
    @MongoId
    UUID id;
    /**
     * The User-geared Account of the submitter
     */
    @Indexed
    @Field("_creator")
    UUID creator;

    /**
     * The User-geared Account if the submitter is using a regular brand account
     */
    @Indexed
    @Field("_uCreator")
    UUID uCreator;

    /**
     * The Brand account they were using when submitting the Falsehood
     */
    UUID showBrand;

    /**
     * The Display name of the submitter
     */
    String authorDisplayName;

    /**
     * When the Falsehood was made
     */
    @JsonFormat(pattern = "MM/dd/yyyy")
    Date dateMade;

    /**
     * The title of the falsehood
     */
    String title;

    /**
     * Any Notes regarding the Falsehood (such as a Brand not yet available in the database)
     */
    String notes;

    /**
     * List of Brands that could constitute the culprits
     */
    @Indexed
    List<UUID> culprits = new ArrayList<>();

    /**
     * List of targets of the Falsehood
     */
    @Indexed
    List<UUID> targets = new ArrayList<>();
}

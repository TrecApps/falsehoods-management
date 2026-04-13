package com.trecapps.falsehoods.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class FalsehoodRet {
    /**
     * The main ID of the Falsehood
     */
    UUID id;
    /**
     * The User-geared Account of the submitter
     */
    UUID creator;

    /**
     * The User-geared Account if the submitter is using a regular brand account
     */
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
    List<Brand> culprits = new ArrayList<>();

    /**
     * List of targets of the Falsehood
     */
    List<Brand> targets = new ArrayList<>();

    /**
     * Stage the Falsehood entry is currently in
     */
    FalsehoodStage status;

    /**
     * The severity of the falsehood
     */
    FalsehoodSeverity severity;
}

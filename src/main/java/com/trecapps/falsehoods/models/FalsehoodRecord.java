package com.trecapps.falsehoods.models;

import com.trecauth.common.model.Record;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.UUID;

/**
 * Mongo Support for Resource Records
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FalsehoodRecord extends Record {
    /**
     * Apply Mongo ID marking to the id field
     */
    @MongoId
    UUID id;
    /**
     * Apply indexing to the resourceId (the falsehood, for easier record finding)
     */
    @Indexed(name = "f_resource_id")
    UUID resourceId;
}

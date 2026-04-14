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

    public Record toRecord(){
        Record ret = new Record();

        ret.setType(this.getType());
        ret.setCreator(this.getCreator());
        ret.setResourceId(this.getResourceId());
        ret.setUCreator(this.getUCreator());
        ret.setMade(this.getMade());
        ret.setMessages(this.getMessages());
        ret.setId(this.id);

        return ret;
    }

    public static FalsehoodRecord getInstance(Record record){
        FalsehoodRecord ret = new FalsehoodRecord();

        ret.setType(record.getType());
        ret.setCreator(record.getCreator());
        ret.setResourceId(record.getResourceId());
        ret.setUCreator(record.getUCreator());
        ret.setMade(record.getMade());
        ret.setMessages(record.getMessages());
        ret.setId(record.getId());

        return ret;
    }
}

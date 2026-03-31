package com.trecapps.falsehoods.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@Data
public class Brief implements Comparable<Brief> {
    public int compareTo(Brief other){
        return created.compareTo(other.created);
    }
    @MongoId
    UUID id;
    String falsehoodId;


    UUID userId;
    UUID brandId;
    Integer version;
    String displayName;
    BriefPurpose purpose;
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss Z")
    Instant created;

    SortedSet<ContentVersion> content = new TreeSet<>();

    public void filter(UUID userId){
        if(userId != null && (userId.equals(this.userId) || userId.equals(this.brandId)))
            return;

        // Just return the last version if not owned by the creator or
        if(content.size() > 1){
            Optional<ContentVersion> contentElement = HelperMethods.getLast(content);
            content = new TreeSet<>();
            contentElement.ifPresent(ContentVersion -> content.add(ContentVersion));
        }
    }
}

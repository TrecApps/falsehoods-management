package com.trecapps.falsehoods.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@Data
public class BriefRet implements Comparable<BriefRet> {
    public int compareTo(BriefRet other){
        return created.compareTo(other.created);
    }
    UUID id;
    UUID falsehoodId;


    UUID uAccount;
    UUID account;
    Integer version;
    String displayName;
    BriefPurpose purpose;
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss Z")
    OffsetDateTime created;

    SortedSet<ContentVersionRet> content = new TreeSet<>();

    public void filter(UUID userId){
        if(userId != null && (userId.equals(this.uAccount) || userId.equals(this.account)))
            return;

        // Just return the last version if not owned by the creator or
        if(content.size() > 1){
            Optional<ContentVersionRet> contentElement = HelperMethods.getLast(content);
            content = new TreeSet<>();
            contentElement.ifPresent(ContentVersion -> content.add(ContentVersion));
        }
    }
}

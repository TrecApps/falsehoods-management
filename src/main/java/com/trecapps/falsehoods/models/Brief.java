package com.trecapps.falsehoods.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    UUID falsehoodId;


    UUID uAccount;
    UUID account;
    Integer version;
    String displayName;
    BriefPurpose purpose;
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss Z")
    Instant created;

    SortedSet<ContentVersion> content = new TreeSet<>();

    public BriefRet toReturnableObj(){
        BriefRet ret = new BriefRet();
        ret.id = this.id;
        ret.falsehoodId = this.falsehoodId;
        ret.uAccount = this.uAccount;
        ret.account = this.account;
        ret.version = this.version;
        ret.displayName = this.displayName;
        ret.purpose = this.purpose;
        ret.created = OffsetDateTime.ofInstant(this.created, ZoneId.systemDefault());

        for(ContentVersion cv : content){
            ret.content.add(
                    new ContentVersionRet(
                            cv.version(),
                            OffsetDateTime.ofInstant(cv.made(), ZoneId.systemDefault()),
                            cv.contents()));
        }

        return ret;
    }

    public void filter(UUID userId){
        if(userId != null && (userId.equals(this.uAccount) || userId.equals(this.account)))
            return;

        // Just return the last version if not owned by the creator or
        if(content.size() > 1){
            Optional<ContentVersion> contentElement = HelperMethods.getLast(content);
            content = new TreeSet<>();
            contentElement.ifPresent(ContentVersion -> content.add(ContentVersion));
        }
    }

    public void update(String newContents){
        Instant now = Instant.now();
        Optional<ContentVersion> contentElement = HelperMethods.getLast(content);
        int newVersion = contentElement.map(ContentVersion -> ContentVersion.version() + 1).orElse(1);
        content.add(new ContentVersion(newVersion, now, newContents));
    }
}

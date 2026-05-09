package com.trecapps.falsehoods.models;

import lombok.Data;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@Data
public class FalsehoodFull {

    FalsehoodDocument metadata;
    FalsehoodRet fullMetaData;
    SortedSet<ContentVersion> content = new TreeSet<>();
    String initContent;

    SortedSet<BriefRet> briefs = new TreeSet<>();

    public void prepReturn(){
        this.metadata = null;
    }

    public void initConvert(){
        initConvert(ZoneId.systemDefault());
    }
    public void initConvert(ZoneId zone){
        if(initContent != null)
            addContentVersion(initContent, zone);
    }

    public void addContentVersion(String newContents, ZoneId zone){
        Instant now = Instant.now();
        Optional<ContentVersion> contentElement = HelperMethods.getLast(content);
        int newVersion = contentElement.map(ContentVersion -> ContentVersion.version() + 1).orElse(1);
        content.add(new ContentVersion(newVersion, now, newContents));
    }

    /**
     * Filters out previous versions
     * @param userId the id of the person requesting the data (use null if not authenticated)
     */
    public void filter(UUID userId){

        for(BriefRet brief: briefs)
            brief.filter(userId);

        if(userId == null || !userId.equals(metadata.uCreator)) {
            if(content.size() > 1){
                Optional<ContentVersion> contentElement = HelperMethods.getLast(content);
                content = new TreeSet<>();
                contentElement.ifPresent(ContentVersion -> content.add(ContentVersion));
            }
        }

    }
}

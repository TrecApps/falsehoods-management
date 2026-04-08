package com.trecapps.falsehoods.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Portions of Brand Data intended to be kept on Storage Services
 */
@Data
public class BrandContent {
    /**
     * The article-style content about a given brand - in markdown format
     */
    String content;
    /**
     * The Main image to present (as base64)
     */
    String imageData;
    /**
     * Profile Description
     */
    String imageDescription;
    /**
     * Metadata to put in a table
     */
    Map<String, String> metadata;

    /**
     * Provides a version of the Metadata in a list form that Thymeleaf can be used
     * @return a List<Entry> of the metadata</Entry>
     */
    public List<Entry> getMetaDataAsEntries(){
        List<Entry> ret = new ArrayList<>(metadata.size());

        metadata.forEach((String key, String value) -> {
            Entry entry = new Entry();
            entry.setKey(key);
            entry.setValue(value);
            ret.add(entry);
        });
        return ret;
    }

    /**
     * Updates the Brand Content details with a new version
     * @param brandContent
     */
    public void update(BrandContent brandContent){
        if(brandContent.content != null)
            content = brandContent.content;

        if(brandContent.imageData != null)
            imageData = brandContent.imageData;

        if(brandContent.imageDescription != null)
            imageDescription = brandContent.imageDescription;


        brandContent.metadata.forEach((String key, String value) -> {
            if("[Remove]".equals(value))
                metadata.remove(key);
            else{
                metadata.put(key, value);
            }
        });
    }
}

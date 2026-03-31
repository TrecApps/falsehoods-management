package com.trecapps.falsehoods.models;

import lombok.Data;

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
}

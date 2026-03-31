package com.trecapps.falsehoods.models;

import java.time.Instant;

public record ContentVersion (int version, Instant made, String contents) implements Comparable<ContentVersion>{
    public int compareTo(ContentVersion other){
        return this.made.compareTo(other.made);
    }

}

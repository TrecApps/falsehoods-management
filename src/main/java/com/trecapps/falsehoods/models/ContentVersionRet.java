package com.trecapps.falsehoods.models;

import java.time.OffsetDateTime;

public record ContentVersionRet(int version, OffsetDateTime made, String contents)  implements Comparable<ContentVersionRet>{
    public int compareTo(ContentVersionRet other){
        return this.made.compareTo(other.made);
    }
}

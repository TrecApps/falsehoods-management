package com.trecapps.falsehoods.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class BrandFalsehoodCount {
    @Data
    public static class SeverityCount {
        String severity;
        long count;
    }

    List<SeverityCount> acceptedCulpritCount = new ArrayList<>();
    List<SeverityCount> confirmedCulpritCount = new ArrayList<>();
    List<SeverityCount> acceptedTargetCount = new ArrayList<>();
    List<SeverityCount> confirmedTargetName = new ArrayList<>();

    private HashMap<String, Long> createListAsMap(List<SeverityCount> list){
        HashMap<String, Long> ret = new HashMap<>();
        for(SeverityCount sc: list){
            ret.put(sc.severity, sc.count);
        }
        return ret;
    }

    public HashMap<String, Long> retrieveAcceptedCulpritCountAsMap(){
        return createListAsMap(acceptedCulpritCount);
    }

    public HashMap<String, Long> retrieveConfirmedCulpritCountAsMap(){
        return createListAsMap(confirmedCulpritCount);
    }

    public HashMap<String, Long> retrieveAcceptedTargetCountAsMap(){
        return createListAsMap(acceptedTargetCount);
    }

    public HashMap<String, Long> retrieveConfirmedTargetNameAsMap(){
        return createListAsMap(confirmedTargetName);
    }
}

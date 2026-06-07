package com.trecapps.falsehoods.models;

import lombok.Data;

import java.util.Map;

@Data
public class BrandFalsehoodTable {
    Map<String, Long> acceptedCulpritCount;
    Map<String, Long> confirmedCulpritCount;
    Map<String, Long> acceptedTargetCount;
    Map<String, Long> confirmedTargetCount;


    public BrandFalsehoodTable(BrandFalsehoodCount counter){
        acceptedCulpritCount = counter.retrieveAcceptedCulpritCountAsMap();
        confirmedCulpritCount = counter.retrieveConfirmedCulpritCountAsMap();
        acceptedTargetCount = counter.retrieveAcceptedTargetCountAsMap();
        confirmedTargetCount = counter.retrieveConfirmedTargetNameAsMap();

        for(FalsehoodSeverity fs: FalsehoodSeverity.values()){
            if(!acceptedCulpritCount.containsKey(fs.name()))
                acceptedCulpritCount.put(fs.name(), 0L);
            if(!confirmedCulpritCount.containsKey(fs.name()))
                confirmedCulpritCount.put(fs.name(), 0L);
            if(!acceptedTargetCount.containsKey(fs.name()))
                acceptedTargetCount.put(fs.name(), 0L);
            if(!confirmedTargetCount.containsKey(fs.name()))
                confirmedTargetCount.put(fs.name(), 0L);
        }
    }
}

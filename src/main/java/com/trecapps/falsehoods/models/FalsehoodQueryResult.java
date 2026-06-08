package com.trecapps.falsehoods.models;

import lombok.Data;

import java.util.List;

@Data
public class FalsehoodQueryResult {


    List<FalsehoodRet> results;
    List<ResultCount> totalCount;

    public long getTotalCountPrimitive(){
        long ret = 0;
        if(totalCount != null)
            for(ResultCount c : totalCount){
                ret += c.getCount();
            }
        return ret;
    }
}

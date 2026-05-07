package com.trecapps.falsehoods.models;

import lombok.Data;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class FalsehoodSearch {
    UUID culprit;
    UUID target;
    Date before;
    Date after;
    FalsehoodSeverity severity;

    public Criteria generateCriteria(FalsehoodStage stage){
        List<Criteria> criteriaList = new ArrayList<>();

        if(culprit != null)
            criteriaList.add(Criteria.where("culprits").is(culprit));
        if(target != null)
            criteriaList.add(Criteria.where("targets").is(target));
        if(before != null)
            criteriaList.add(Criteria.where("dateMade").lte(before));
        if(after != null)
            criteriaList.add(Criteria.where("dateMade").gte(after));
        if(severity != null)
            criteriaList.add(Criteria.where("severity").is(severity));

        if(stage == null)
            stage = FalsehoodStage.CONFIRMED;
        criteriaList.add(Criteria.where("status").is(stage));

        return new Criteria().andOperator(criteriaList);
    }
}

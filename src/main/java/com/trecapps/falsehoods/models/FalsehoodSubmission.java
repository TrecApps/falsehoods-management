package com.trecapps.falsehoods.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.*;

@Data
public class FalsehoodSubmission {
    List<UUID> culprits = new ArrayList<>();
    List<UUID> targets = new ArrayList<>();

    Date dateMade;

    @NotNull
    FalsehoodSeverity severity;

    @NotNull
    String title;


    @NotNull String content;

    String notes;

    boolean showRegularAccount;

    public FalsehoodDocument convertToDoc(UUID id, boolean doSubmit){
        FalsehoodDocument ret = new FalsehoodDocument();
        ret.status = doSubmit ? FalsehoodStage.SUBMITTED : FalsehoodStage.SAVED;
        ret.id = id;
        ret.dateMade = dateMade;
        ret.notes = notes;
        ret.severity = severity;

        ret.getCulprits().addAll(culprits);

        ret.title = title;

        return ret;
    }
}

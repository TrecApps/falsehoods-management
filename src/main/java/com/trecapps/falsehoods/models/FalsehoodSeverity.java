package com.trecapps.falsehoods.models;

public enum FalsehoodSeverity {
    OBJECTIVE,                          // The given claim is objectively false
    OPPOSING_EVIDENCE_WITHHELD,         // The claim is undermined by evidence hidden from the audience/readers
    SUPPORTING_EVIDENCE_WITHHELD,       // There was supporting evidence, but it was withheld
    // (possibly to mislead the services contributors)
    SUBJECTIVE,                         // The given claim uses a standard not consistently used by the person or organization served
    NARRATIVE_ISSUE,                    // (MISC - avoid until clear guidance is set)
    FAULTY_LOGIC,                       // The logic used to support a narrative is faulty and can be undermined with solid logic
    TITLE_OR_SLOGAN                     // A chronic falsehood where an entity engages in false advertising or name
}
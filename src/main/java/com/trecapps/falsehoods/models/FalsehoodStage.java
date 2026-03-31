package com.trecapps.falsehoods.models;

public enum FalsehoodStage {
    SAVED,  // The owner is currently editing it, do not reveal in search results
    SUBMITTED,  // The owner has submitted it, awaiting the first round of review
    REJECTED,   // It has been rejected in the first round of review
    R_APPEALED, // It has been appealed after the rejection stage
    ACCEPTED,   // It has passed the first round of review
    DENIED,     // It has failed the second round of review
    CONFIRMED,  // It has passed the second round of review
    S_APPEALED,  // The previous Second review status is being appealed
    DELETED
}

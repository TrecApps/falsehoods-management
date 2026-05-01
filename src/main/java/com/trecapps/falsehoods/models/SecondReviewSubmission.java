package com.trecapps.falsehoods.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public
class SecondReviewSubmission {
    @JsonProperty(required = true)
    UUID falsehoodId;

    @JsonProperty(required = true)
    String comment;
    Integer denyPoints;
}

package com.trecapps.falsehoods.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FalsehoodPatch {
    @NotNull
    String field;

    String value;

    List<String> values = new ArrayList<>();
}

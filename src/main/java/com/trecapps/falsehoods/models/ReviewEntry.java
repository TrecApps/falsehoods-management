package com.trecapps.falsehoods.models;

import lombok.Data;

import java.util.UUID;

@Data
public class ReviewEntry {
    boolean approve;
    String comment;
    UUID id;
}

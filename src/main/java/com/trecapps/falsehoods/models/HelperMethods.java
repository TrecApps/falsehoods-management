package com.trecapps.falsehoods.models;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedSet;

public class HelperMethods {

    public static <T> Optional<T> getLast(SortedSet<T> set){
        try {
            return Optional.of(set.getLast());
        } catch(NoSuchElementException | NullPointerException ignore){
            return Optional.empty();
        }
    }
}

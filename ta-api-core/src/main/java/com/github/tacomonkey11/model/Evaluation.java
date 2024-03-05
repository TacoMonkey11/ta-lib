package com.github.tacomonkey11.model;

import java.util.List;

/**
 *
 * @param name
 * @param categories
 * @param v4 The version of teachassist. There will only be two category if true: term and culm.
 */
public record Evaluation(String name, List<Category> categories, boolean v4) {
    public record Category(String name, double mark, double weight){}
}

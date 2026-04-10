package com.nomnomnow.nnnbackend.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum SuperCategories {
    SEASONAL(1L, "seasonal"),
    MAININGREDIENT(2L, "mainingredient"),
    ORIGIN(3L, "origin"),
    COURSE(4L, "course"),
    SPECIAL(5L, "special"),
    DRINKS(6L, "drinks"),
    PREPARATIONMETHODE(7L, "preparationmethod"),
    DIETARYTYPE(8L, "dietarytype");

    private final long id;
    private final String name;

    SuperCategories(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static List<SuperCategories> getAll() {
        return Arrays.asList(values());
    }
}
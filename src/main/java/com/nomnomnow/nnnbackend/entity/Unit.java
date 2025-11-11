package com.nomnomnow.nnnbackend.entity;

public enum Unit {

    GRAM("g"),
    KILOGRAM("kg"),
    MILLILITER("ml"),
    LITER("l"),
    PIECE("pcs"),
    TEASPOON("tsp"),
    TABLESPOON("tbsp");

    private final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}

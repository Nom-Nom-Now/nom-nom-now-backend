package com.nomnomnow.nnnbackend.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum Categories {

    SPRING(1L, SuperCategories.SEASONAL.getId(), "spring"),
    SUMMER(2L, SuperCategories.SEASONAL.getId(), "summer"),
    AUTUMN(3L, SuperCategories.SEASONAL.getId(), "autumn"),
    WINTER(4L, SuperCategories.SEASONAL.getId(), "winter"),
    ALL_SEASON(5L, SuperCategories.SEASONAL.getId(), "allSeason"),

    MEAT(6L, SuperCategories.MAININGREDIENT.getId(), "meat"),
    FISH(7L, SuperCategories.MAININGREDIENT.getId(), "fish"),
    VEGETABLES(8L, SuperCategories.MAININGREDIENT.getId(), "vegetables"),
    CHEESE(9L, SuperCategories.MAININGREDIENT.getId(), "cheese"),
    RICE(10L, SuperCategories.MAININGREDIENT.getId(), "rice"),
    POTATOES(11L, SuperCategories.MAININGREDIENT.getId(), "potatoes"),
    PASTA(12L, SuperCategories.MAININGREDIENT.getId(), "pasta"),
    EGGS(13L, SuperCategories.MAININGREDIENT.getId(), "eggs"),
    TOFU(14L, SuperCategories.MAININGREDIENT.getId(), "tofu"),
    LEGUMES(15L, SuperCategories.MAININGREDIENT.getId(), "legumes"),

    ITALIAN(16L, SuperCategories.ORIGIN.getId(), "italian"),
    ASIAN(17L, SuperCategories.ORIGIN.getId(), "asian"),
    GERMAN(18L, SuperCategories.ORIGIN.getId(), "german"),
    BAVARIAN(19L, SuperCategories.ORIGIN.getId(), "bavarian"),
    FRENCH(20L, SuperCategories.ORIGIN.getId(), "french"),
    MEXICAN(21L, SuperCategories.ORIGIN.getId(), "mexican"),
    MEDITERRANEAN(22L, SuperCategories.ORIGIN.getId(), "mediterranean"),
    AMERICAN(23L, SuperCategories.ORIGIN.getId(), "american"),
    INDIAN(24L, SuperCategories.ORIGIN.getId(), "indian"),
    ORIENTAL(25L, SuperCategories.ORIGIN.getId(), "oriental"),

    BREAKFAST(26L, SuperCategories.COURSE.getId(), "breakfast"),
    BRUNCH(27L, SuperCategories.COURSE.getId(), "brunch"),
    LUNCH(28L, SuperCategories.COURSE.getId(), "lunch"),
    DINNER(29L, SuperCategories.COURSE.getId(), "dinner"),
    STARTER(30L, SuperCategories.COURSE.getId(), "starter"),
    MAIN_COURSE(31L, SuperCategories.COURSE.getId(), "mainCourse"),
    DESSERT(32L, SuperCategories.COURSE.getId(), "dessert"),
    SNACK(33L, SuperCategories.COURSE.getId(), "snack"),

    CASSEROLES(34L, SuperCategories.SPECIAL.getId(), "casseroles"),
    SOUPS(35L, SuperCategories.SPECIAL.getId(), "soups"),
    SALADS(36L, SuperCategories.SPECIAL.getId(), "salads"),
    FINGER_FOOD(37L, SuperCategories.SPECIAL.getId(), "fingerFood"),
    BREAD(38L, SuperCategories.SPECIAL.getId(), "bread"),
    CAKES(39L, SuperCategories.SPECIAL.getId(), "cakes"),
    PASTRIES(40L, SuperCategories.SPECIAL.getId(), "pastries"),
    SAUCES(41L, SuperCategories.SPECIAL.getId(), "sauces"),
    DIPS(42L, SuperCategories.SPECIAL.getId(), "dips"),
    SIDE_DISHES(43L, SuperCategories.SPECIAL.getId(), "sideDishes"),

    WATER_BASED(44L, SuperCategories.DRINKS.getId(), "waterBased"),
    JUICE(45L, SuperCategories.DRINKS.getId(), "juice"),
    SMOOTHIE(46L, SuperCategories.DRINKS.getId(), "smoothie"),
    MILKSHAKE(47L, SuperCategories.DRINKS.getId(), "milkshake"),
    COFFEE(48L, SuperCategories.DRINKS.getId(), "coffee"),
    TEA(49L, SuperCategories.DRINKS.getId(), "tea"),
    COCKTAIL(50L, SuperCategories.DRINKS.getId(), "cocktail"),
    MOCKTAIL(51L, SuperCategories.DRINKS.getId(), "mocktail"),
    ALCOHOLIC(52L, SuperCategories.DRINKS.getId(), "alcoholic"),
    NON_ALCOHOLIC(53L, SuperCategories.DRINKS.getId(), "nonAlcoholic"),

    BAKE(54L, SuperCategories.PREPARATIONMETHODE.getId(), "bake"),
    COOK(55L, SuperCategories.PREPARATIONMETHODE.getId(), "cook"),
    ROAST(56L, SuperCategories.PREPARATIONMETHODE.getId(), "roast"),
    GRILL(57L, SuperCategories.PREPARATIONMETHODE.getId(), "grill"),
    STEAM(58L, SuperCategories.PREPARATIONMETHODE.getId(), "steam"),
    FRY(59L, SuperCategories.PREPARATIONMETHODE.getId(), "fry"),
    BOIL(60L, SuperCategories.PREPARATIONMETHODE.getId(), "boil"),
    NO_BAKE(61L, SuperCategories.PREPARATIONMETHODE.getId(), "noBake"),
    SLOW_COOK(62L, SuperCategories.PREPARATIONMETHODE.getId(), "slowCook"),
    AIR_FRY(63L, SuperCategories.PREPARATIONMETHODE.getId(), "airFry"),

    VEGETARIAN(64L, SuperCategories.DIETARYTYPE.getId(), "vegetarian"),
    VEGAN(65L, SuperCategories.DIETARYTYPE.getId(), "vegan"),
    LIGHT(66L, SuperCategories.DIETARYTYPE.getId(), "light"),
    DIET(67L, SuperCategories.DIETARYTYPE.getId(), "diet"),
    HIGH_PROTEIN(68L, SuperCategories.DIETARYTYPE.getId(), "highProtein"),
    GLUTEN_FREE(69L, SuperCategories.DIETARYTYPE.getId(), "glutenFree"),
    LACTOSE_FREE(70L, SuperCategories.DIETARYTYPE.getId(), "lactoseFree"),
    LOW_CARB(71L, SuperCategories.DIETARYTYPE.getId(), "lowCarb"),
    SUGAR_FREE(72L, SuperCategories.DIETARYTYPE.getId(), "sugarFree"),
    KETO(73L, SuperCategories.DIETARYTYPE.getId(), "keto");

    private final long id;
    private final long superCategoryId;
    private final String name;

    Categories(long id, long superCategoryId, String name) {
        this.id = id;
        this.superCategoryId = superCategoryId;
        this.name = name;
    }

    public static List<Categories> getAll() {
        return Arrays.asList(values());
    }
}
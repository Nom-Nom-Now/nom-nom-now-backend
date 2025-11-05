package com.nomnomnow.nnnbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class RecipeCategoryId implements Serializable {

    @Column(name = "recipe_id") private Long recipeId;
    @Column(name = "category_id") private Long categoryId;

}

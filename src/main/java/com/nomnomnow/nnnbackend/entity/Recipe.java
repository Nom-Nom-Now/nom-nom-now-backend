package com.nomnomnow.nnnbackend.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Set;

@Entity
@Data
@Table(name = "recipe", schema = "app")
@RequiredArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String instructions;
    private int cookingTime;

    @ElementCollection
    @CollectionTable(name = "recipe_categories", schema = "app", joinColumns = @JoinColumn(name = "recipe_id"))
    private Set<String> categories;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecipeComponent> components;

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof Recipe recipe))
            return false;
        return Objects.equals(name, recipe.name); //Der Name eines Rezeptes sollte eindeutig sein.
    }
}

package com.nomnomnow.nnnbackend.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "recipe", schema = "app")
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "components")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String instructions;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    @ManyToMany
    @JoinTable(
            name = "recipe_category",
            schema = "app",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecipeComponent> components = new HashSet<>();
}

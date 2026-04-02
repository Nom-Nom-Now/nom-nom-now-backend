package com.nomnomnow.nnnbackend.entity;


import com.nomnomnow.nnnbackend.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "recipe", schema = "app")
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"components", "owner"})
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String instructions;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @ManyToMany
    @JoinTable(
            name = "recipe_category",
            schema = "app",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @BatchSize(size = 20)
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<RecipeComponent> components = new HashSet<>();
}

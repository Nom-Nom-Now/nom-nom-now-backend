package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Recipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"categories", "components", "components.ingredient"})
    List<Recipe> findAll();
}

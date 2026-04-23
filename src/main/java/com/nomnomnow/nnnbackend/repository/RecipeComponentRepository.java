package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeComponentRepository extends JpaRepository<RecipeComponent, Long> {

    boolean existsByIngredientId(Long ingredientId);
}


package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"components.ingredient"})
    Page<Recipe> findAll(Pageable pageable);
}

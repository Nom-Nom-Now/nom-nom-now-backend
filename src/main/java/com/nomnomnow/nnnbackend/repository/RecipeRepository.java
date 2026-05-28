package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"components.ingredient"})
    Page<Recipe> findAll(Pageable pageable);

    boolean existsByName(String name);

    @Query("SELECT r FROM Recipe r ORDER BY function('random')")
    List<Recipe> findRandomRecipes(Pageable pageable);
}

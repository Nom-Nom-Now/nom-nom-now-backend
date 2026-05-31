package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"components.ingredient"})
    Page<Recipe> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"components.ingredient"})
    Page<Recipe> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"components.ingredient"})
    @Query("""
        SELECT r FROM Recipe r
        WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
           OR r.id IN (
             SELECT rc.recipe.id FROM RecipeComponent rc
             JOIN rc.ingredient i
             WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
           )
    """)
    Page<Recipe> searchByNameOrIngredient(@Param("q") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"components.ingredient"})
    @Query("""
        SELECT r FROM Recipe r
        WHERE r.owner.id = :ownerId
          AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
               OR r.id IN (
                 SELECT rc.recipe.id FROM RecipeComponent rc
                 JOIN rc.ingredient i
                 WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
               ))
    """)
    Page<Recipe> searchByOwnerAndNameOrIngredient(
            @Param("ownerId") Long ownerId,
            @Param("q") String query,
            Pageable pageable);

    boolean existsByName(String name);

    @Query("SELECT r FROM Recipe r ORDER BY function('random')")
    List<Recipe> findRandomRecipes(Pageable pageable);

    @Query("""
        SELECT r FROM Recipe r
        WHERE r.id <> :excludedRecipeId
        ORDER BY function('random')
    """)
    List<Recipe> findRandomRecipesExcluding(@Param("excludedRecipeId") Long excludedRecipeId, Pageable pageable);
}

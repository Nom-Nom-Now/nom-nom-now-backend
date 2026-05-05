package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);

    @Modifying
    @Query("DELETE FROM Ingredient i WHERE i.id IN :ids AND NOT EXISTS (SELECT 1 FROM RecipeComponent rc WHERE rc.ingredient.id = i.id)")
    long deleteOrphanedByIds(@Param("ids") Set<Long> ids);
}

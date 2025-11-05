package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

}

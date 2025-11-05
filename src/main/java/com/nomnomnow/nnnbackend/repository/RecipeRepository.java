package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long>{

}

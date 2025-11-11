package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.Category;
import com.nomnomnow.nnnbackend.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository <Category, Long>{
    Optional<Category> findByNameIgnoreCase(String name);
}

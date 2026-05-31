package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.ShoppingList;
import com.nomnomnow.nnnbackend.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    @EntityGraph(attributePaths = "items")
    @Query("SELECT DISTINCT sl FROM ShoppingList sl WHERE sl.owner = :owner ORDER BY sl.createdAt DESC")
    List<ShoppingList> findByOwnerOrderByCreatedAtDesc(@Param("owner") AppUser owner);

    @EntityGraph(attributePaths = "items")
    Optional<ShoppingList> findByIdAndOwner(Long id, AppUser owner);
}

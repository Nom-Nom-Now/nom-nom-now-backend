package com.nomnomnow.nnnbackend.repository;

import com.nomnomnow.nnnbackend.entity.RecipePlan;
import com.nomnomnow.nnnbackend.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecipePlanRepository extends JpaRepository<RecipePlan, Long> {

    @Query("SELECT rp FROM RecipePlan rp " +
           "LEFT JOIN FETCH rp.recipe r " +
           "LEFT JOIN FETCH r.components " +
           "WHERE rp.owner = :owner " +
           "AND rp.planDate BETWEEN :startDate AND :endDate " +
           "ORDER BY rp.planDate")
    List<RecipePlan> findByOwnerAndDateRange(
            @Param("owner") AppUser owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByOwnerAndPlanDateBetween(AppUser owner, LocalDate startDate, LocalDate endDate);

    Optional<RecipePlan> findByOwnerAndPlanDate(AppUser owner, LocalDate planDate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteByOwnerAndPlanDateBetween(AppUser owner, LocalDate startDate, LocalDate endDate);
}

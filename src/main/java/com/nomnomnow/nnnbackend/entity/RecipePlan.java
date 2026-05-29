package com.nomnomnow.nnnbackend.entity;

import com.nomnomnow.nnnbackend.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "recipe_plan", schema = "app", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "plan_date"}))
@RequiredArgsConstructor
public class RecipePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

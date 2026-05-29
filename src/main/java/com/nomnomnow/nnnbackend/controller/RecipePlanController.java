package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.dto.request.RecipePlanRequest;
import com.nomnomnow.nnnbackend.dto.response.RecipePlanResponse;
import com.nomnomnow.nnnbackend.service.RecipePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recipe-plans")
@RequiredArgsConstructor
public class RecipePlanController {

    private final RecipePlanService recipePlanService;

    @GetMapping
    public ResponseEntity<List<RecipePlanResponse>> getWeeklyPlan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(recipePlanService.getWeeklyPlan(weekStart));
    }

    @PostMapping
    public ResponseEntity<List<RecipePlanResponse>> saveWeeklyPlan(
            @Valid @RequestBody RecipePlanRequest request) {
        return ResponseEntity.ok(recipePlanService.saveWeeklyPlan(request));
    }
}

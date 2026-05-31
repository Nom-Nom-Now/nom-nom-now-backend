package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.RecipePlanRequest;
import com.nomnomnow.nnnbackend.dto.response.RecipePlanResponse;
import com.nomnomnow.nnnbackend.dto.response.RecipeResponse;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipePlan;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.mapper.RecipeMapper;
import com.nomnomnow.nnnbackend.repository.RecipePlanRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;

@Service
@RequiredArgsConstructor
public class RecipePlanService {

    private static final long DAYS_IN_WEEK = 7L;

    private final RecipePlanRepository recipePlanRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public List<RecipePlanResponse> getWeeklyPlan(LocalDate weekStart) {
        return mapToResponses(getOrCreateWeeklyPlansForCurrentUser(weekStart));
    }

    @Transactional
    public List<RecipePlan> getOrCreateWeeklyPlansForCurrentUser(LocalDate weekStart) {
        AppUser currentUser = currentUserService.getCurrentUser();
        LocalDate normalizedWeekStart = normalizeWeekStart(weekStart);
        validateWeekAccess(currentUser, normalizedWeekStart);
        LocalDate weekEnd = normalizedWeekStart.plusDays(DAYS_IN_WEEK - 1);

        List<RecipePlan> plans = recipePlanRepository.findByOwnerAndDateRange(
                currentUser, normalizedWeekStart, weekEnd);

        if (plans.isEmpty()) {
            plans = generateWeeklyPlan(currentUser, normalizedWeekStart);
        }

        return plans;
    }

    @Transactional
    public List<RecipePlanResponse> saveWeeklyPlan(RecipePlanRequest request) {
        AppUser currentUser = currentUserService.getCurrentUser();
        LocalDate normalizedWeekStart = normalizeWeekStart(request.weekStart());
        validateWeekRefreshAccess(normalizedWeekStart);

        LocalDate weekEnd = normalizedWeekStart.plusDays(DAYS_IN_WEEK - 1);

        List<Recipe> recipes = request.recipeIds().stream()
                .map(this::findRecipeById)
                .toList();

        recipePlanRepository.deleteByOwnerAndPlanDateBetween(
                currentUser, normalizedWeekStart, weekEnd);
        recipePlanRepository.flush();

        List<RecipePlan> savedPlans = recipePlanRepository.saveAll(
                createPlans(currentUser, recipes, normalizedWeekStart)
        );

        return mapToResponses(savedPlans);
    }

    @Transactional
    public RecipePlanResponse refreshPlanDay(LocalDate planDate) {
        AppUser currentUser = currentUserService.getCurrentUser();
        validateWeekRefreshAccess(normalizeWeekStart(planDate));

        RecipePlan plan = recipePlanRepository.findByOwnerAndPlanDate(currentUser, planDate)
                .orElseGet(() -> createPlan(currentUser, planDate));
        Long currentRecipeId = plan.getRecipe() != null ? plan.getRecipe().getId() : null;

        plan.setRecipe(findRandomRecipeForDay(currentRecipeId));

        return mapToResponse(recipePlanRepository.save(plan));
    }

    private List<RecipePlan> generateWeeklyPlan(AppUser currentUser, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(DAYS_IN_WEEK - 1);

        if (recipePlanRepository.existsByOwnerAndPlanDateBetween(currentUser, weekStart, weekEnd)) {
            return recipePlanRepository.findByOwnerAndDateRange(currentUser, weekStart, weekEnd);
        }

        List<Recipe> randomRecipes = recipeRepository.findRandomRecipes(PageRequest.of(0, (int) DAYS_IN_WEEK));

        if (randomRecipes.isEmpty()) {
            return List.of();
        }

        return recipePlanRepository.saveAll(createPlans(currentUser, randomRecipes, weekStart));
    }

    private Recipe findRecipeById(Long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
    }

    private Recipe findRandomRecipeForDay(Long currentRecipeId) {
        List<Recipe> randomRecipes = currentRecipeId == null
                ? recipeRepository.findRandomRecipes(PageRequest.of(0, 1))
                : recipeRepository.findRandomRecipesExcluding(currentRecipeId, PageRequest.of(0, 1));

        if (randomRecipes.isEmpty() && currentRecipeId != null) {
            randomRecipes = recipeRepository.findRandomRecipes(PageRequest.of(0, 1));
        }

        if (randomRecipes.isEmpty()) {
            throw new BadRequestException("No recipes available for refreshing this meal plan day");
        }

        return randomRecipes.getFirst();
    }

    private List<RecipePlan> createPlans(AppUser owner, List<Recipe> recipes, LocalDate weekStart) {
        List<RecipePlan> plans = new ArrayList<>();

        for (int i = 0; i < recipes.size(); i++) {
            RecipePlan plan = createPlan(owner, weekStart.plusDays(i));
            plan.setRecipe(recipes.get(i));

            plans.add(plan);
        }

        return plans;
    }

    private RecipePlan createPlan(AppUser owner, LocalDate planDate) {
        RecipePlan plan = new RecipePlan();
        plan.setOwner(owner);
        plan.setPlanDate(planDate);
        return plan;
    }

    private List<RecipePlanResponse> mapToResponses(List<RecipePlan> plans) {
        return plans.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateWeekRefreshAccess(LocalDate weekStart) {
        LocalDate currentWeekStart = normalizeWeekStart(LocalDate.now());
        LocalDate latestAllowedWeekStart = currentWeekStart.plusWeeks(1);

        if (weekStart.isBefore(currentWeekStart)) {
            throw new BadRequestException("Cannot refresh meal plans from past weeks");
        }

        if (weekStart.isAfter(latestAllowedWeekStart)) {
            throw new BadRequestException("Cannot refresh meal plans more than one week into the future");
        }
    }

    private void validateWeekAccess(AppUser currentUser, LocalDate weekStart) {
        LocalDate currentWeekStart = normalizeWeekStart(LocalDate.now());
        LocalDate latestAllowedWeekStart = currentWeekStart.plusWeeks(1);
        LocalDate accountCreationWeekStart = normalizeWeekStart(currentUser.getCreatedAt().toLocalDate());

        if (weekStart.isBefore(accountCreationWeekStart)) {
            throw new BadRequestException("Cannot access meal plans before your account creation date");
        }

        if (weekStart.isAfter(latestAllowedWeekStart)) {
            throw new BadRequestException("Cannot access meal plans more than one week into the future");
        }
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(MONDAY));
    }

    private RecipePlanResponse mapToResponse(RecipePlan plan) {
        RecipeResponse recipeResponse = recipeMapper.toResponse(plan.getRecipe());
        return new RecipePlanResponse(
                plan.getId(),
                plan.getPlanDate(),
                recipeResponse
        );
    }
}

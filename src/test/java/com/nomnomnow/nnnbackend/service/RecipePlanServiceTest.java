package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.response.RecipeResponse;
import com.nomnomnow.nnnbackend.entity.Recipe;
import com.nomnomnow.nnnbackend.entity.RecipePlan;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.mapper.RecipeMapper;
import com.nomnomnow.nnnbackend.repository.RecipePlanRepository;
import com.nomnomnow.nnnbackend.repository.RecipeRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipePlanServiceTest {

    @Mock
    private RecipePlanRepository recipePlanRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private CurrentUserService currentUserService;

    private RecipePlanService recipePlanService;

    @BeforeEach
    void setUp() {
        recipePlanService = new RecipePlanService(
                recipePlanRepository,
                recipeRepository,
                recipeMapper,
                currentUserService
        );
    }

    @Test
    void refreshPlanDayReplacesOnlyTheSelectedDayRecipe() {
        var owner = user(42L);
        var planDate = currentWeekStart().plusDays(2);
        var existingRecipe = recipe(1L, "Old soup");
        var replacementRecipe = recipe(2L, "New pasta");
        var existingPlan = plan(10L, owner, existingRecipe, planDate);
        var responseRecipe = responseRecipe(replacementRecipe);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(recipePlanRepository.findByOwnerAndPlanDate(owner, planDate)).thenReturn(Optional.of(existingPlan));
        when(recipeRepository.findRandomRecipesExcluding(1L, PageRequest.of(0, 1)))
                .thenReturn(List.of(replacementRecipe));
        when(recipePlanRepository.save(existingPlan)).thenReturn(existingPlan);
        when(recipeMapper.toResponse(replacementRecipe)).thenReturn(responseRecipe);

        var response = recipePlanService.refreshPlanDay(planDate);

        assertThat(existingPlan.getRecipe()).isEqualTo(replacementRecipe);
        assertThat(response.planDate()).isEqualTo(planDate);
        assertThat(response.recipe()).isEqualTo(responseRecipe);
        verify(recipePlanRepository, never()).deleteByOwnerAndPlanDateBetween(any(), any(), any());
    }

    @Test
    void refreshPlanDayCreatesTheSelectedDayWhenItDoesNotExistYet() {
        var owner = user(42L);
        var planDate = currentWeekStart().plusDays(4);
        var recipe = recipe(3L, "Curry");

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(recipePlanRepository.findByOwnerAndPlanDate(owner, planDate)).thenReturn(Optional.empty());
        when(recipeRepository.findRandomRecipes(PageRequest.of(0, 1))).thenReturn(List.of(recipe));
        when(recipePlanRepository.save(any(RecipePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeMapper.toResponse(recipe)).thenReturn(responseRecipe(recipe));

        var response = recipePlanService.refreshPlanDay(planDate);

        assertThat(response.planDate()).isEqualTo(planDate);
        assertThat(response.recipe().id()).isEqualTo(3L);
    }

    @Test
    void refreshPlanDayRejectsPastWeeks() {
        var owner = user(42L);

        when(currentUserService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> recipePlanService.refreshPlanDay(currentWeekStart().minusWeeks(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot refresh meal plans from past weeks");

        verify(recipePlanRepository, never()).findByOwnerAndPlanDate(any(), any());
    }

    private AppUser user(Long id) {
        var user = new AppUser();
        user.setId(id);
        return user;
    }

    private Recipe recipe(Long id, String name) {
        var recipe = new Recipe();
        recipe.setId(id);
        recipe.setName(name);
        return recipe;
    }

    private RecipePlan plan(Long id, AppUser owner, Recipe recipe, LocalDate planDate) {
        var plan = new RecipePlan();
        plan.setId(id);
        plan.setOwner(owner);
        plan.setRecipe(recipe);
        plan.setPlanDate(planDate);
        return plan;
    }

    private RecipeResponse responseRecipe(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    }
}

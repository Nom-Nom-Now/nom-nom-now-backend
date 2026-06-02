package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.ShoppingListRequest;
import com.nomnomnow.nnnbackend.dto.request.ShoppingListDayRequest;
import com.nomnomnow.nnnbackend.entity.*;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.ShoppingListRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private RecipePlanService recipePlanService;

    @Mock
    private CurrentUserService currentUserService;

    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        shoppingListService = new ShoppingListService(
                shoppingListRepository,
                recipePlanService,
                currentUserService
        );
    }

    @Test
    void generateShoppingListAggregatesEqualIngredientsWithSameUnit() {
        var owner = user(42L);
        var weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        var pasta = recipe(component("Tomato", "2.50", Unit.PIECE), component("Salt", "1.00", Unit.GRAM));
        var soup = recipe(component("Tomato", "1.50", Unit.PIECE), component("Salt", "1.00", Unit.TEASPOON));

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(recipePlanService.getOrCreateWeeklyPlansForCurrentUser(weekStart))
                .thenReturn(List.of(plan(owner, pasta, weekStart), plan(owner, soup, weekStart.plusDays(1))));
        when(shoppingListRepository.saveAndFlush(any(ShoppingList.class))).thenAnswer(invocation -> {
            var shoppingList = (ShoppingList) invocation.getArgument(0);
            shoppingList.setId(7L);
            shoppingList.setCreatedAt(OffsetDateTime.parse("2026-05-31T12:00:00Z"));
            return shoppingList;
        });

        var response = shoppingListService.generateShoppingList(new ShoppingListRequest(weekStart));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.weekStart()).isEqualTo(weekStart);
        assertThat(response.items()).extracting("ingredientName")
                .containsExactly("Salt", "Salt", "Tomato");
        assertThat(response.items()).extracting("quantity")
                .containsExactly(
                        new BigDecimal("1.00"),
                        new BigDecimal("1.00"),
                        new BigDecimal("4.00")
                );
        assertThat(response.items()).extracting("unit")
                .containsExactly(Unit.GRAM, Unit.TEASPOON, Unit.PIECE);
    }

    @Test
    void generateShoppingListScalesIngredientsByPeopleCountPerPlanDate() {
        var owner = user(42L);
        var weekStart = LocalDate.of(2026, 6, 1);
        var mondayRecipe = recipe(4, component("Tomato", "2.00", Unit.PIECE), component("Salt", "1.00", Unit.GRAM));
        var tuesdayRecipe = recipe(3, component("Tomato", "3.00", Unit.PIECE));

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(recipePlanService.getOrCreateWeeklyPlansForCurrentUser(weekStart))
                .thenReturn(List.of(
                        plan(owner, mondayRecipe, weekStart),
                        plan(owner, tuesdayRecipe, weekStart.plusDays(1))
                ));
        when(shoppingListRepository.saveAndFlush(any(ShoppingList.class))).thenAnswer(invocation -> {
            var shoppingList = (ShoppingList) invocation.getArgument(0);
            shoppingList.setId(7L);
            shoppingList.setCreatedAt(OffsetDateTime.parse("2026-06-01T12:00:00Z"));
            return shoppingList;
        });

        var response = shoppingListService.generateShoppingList(new ShoppingListRequest(
                weekStart,
                List.of(
                        new ShoppingListDayRequest(weekStart, 2),
                        new ShoppingListDayRequest(weekStart.plusDays(1), 3)
                )
        ));

        assertThat(response.items()).extracting("ingredientName")
                .containsExactly("Salt", "Tomato");
        assertThat(response.items()).extracting("quantity")
                .containsExactly(
                        new BigDecimal("0.50"),
                        new BigDecimal("4.00")
                );
    }

    @Test
    void generateShoppingListRejectsPeopleCountsOutsideRequestedWeek() {
        var owner = user(42L);
        var weekStart = LocalDate.of(2026, 6, 1);

        when(currentUserService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> shoppingListService.generateShoppingList(new ShoppingListRequest(
                weekStart,
                List.of(new ShoppingListDayRequest(weekStart.plusDays(7), 2))
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("People counts must belong to the requested week");
    }

    @Test
    void generateShoppingListCreatesANewSnapshotEveryTime() {
        var owner = user(42L);
        var weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        var recipe = recipe(component("Rice", "2.00", Unit.GRAM));

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(recipePlanService.getOrCreateWeeklyPlansForCurrentUser(weekStart))
                .thenReturn(List.of(plan(owner, recipe, weekStart)));
        when(shoppingListRepository.saveAndFlush(any(ShoppingList.class))).thenAnswer(invocation -> {
            var shoppingList = (ShoppingList) invocation.getArgument(0);
            shoppingList.setId(shoppingList.getId() == null ? 1L : shoppingList.getId() + 1L);
            shoppingList.setCreatedAt(OffsetDateTime.parse("2026-05-31T12:00:00Z"));
            return shoppingList;
        });

        shoppingListService.generateShoppingList(new ShoppingListRequest(weekStart));
        shoppingListService.generateShoppingList(new ShoppingListRequest(weekStart));

        verify(shoppingListRepository, org.mockito.Mockito.times(2)).saveAndFlush(any(ShoppingList.class));
    }

    @Test
    void getShoppingListOnlyReturnsListsForCurrentUser() {
        var owner = user(42L);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(shoppingListRepository.findByIdAndOwner(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingListService.getShoppingList(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shopping list not found with id: 99");
    }

    @Test
    void deleteShoppingListDeletesOnlyListsForCurrentUser() {
        var owner = user(42L);
        var shoppingList = new ShoppingList();
        shoppingList.setId(7L);
        shoppingList.setOwner(owner);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(shoppingListRepository.findByIdAndOwner(7L, owner)).thenReturn(Optional.of(shoppingList));

        shoppingListService.deleteShoppingList(7L);

        verify(shoppingListRepository).delete(shoppingList);
    }

    @Test
    void deleteShoppingListThrowsWhenListDoesNotBelongToCurrentUser() {
        var owner = user(42L);

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(shoppingListRepository.findByIdAndOwner(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingListService.deleteShoppingList(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shopping list not found with id: 99");
        verify(shoppingListRepository, never()).delete(any());
    }

    private AppUser user(Long id) {
        var user = new AppUser();
        user.setId(id);
        return user;
    }

    private Recipe recipe(RecipeComponent... components) {
        return recipe(1, components);
    }

    private Recipe recipe(Integer servings, RecipeComponent... components) {
        var recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Recipe");
        recipe.setServings(servings);
        for (RecipeComponent component : components) {
            component.setRecipe(recipe);
            recipe.getComponents().add(component);
        }
        return recipe;
    }

    private RecipeComponent component(String ingredientName, String quantity, Unit unit) {
        var ingredient = new Ingredient();
        ingredient.setName(ingredientName);

        var component = new RecipeComponent();
        component.setIngredient(ingredient);
        component.setQuantity(new BigDecimal(quantity));
        component.setUnit(unit);
        return component;
    }

    private RecipePlan plan(AppUser owner, Recipe recipe, LocalDate planDate) {
        var plan = new RecipePlan();
        plan.setOwner(owner);
        plan.setRecipe(recipe);
        plan.setPlanDate(planDate);
        return plan;
    }
}

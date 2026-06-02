package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.ShoppingListRequest;
import com.nomnomnow.nnnbackend.dto.response.ShoppingListItemResponse;
import com.nomnomnow.nnnbackend.dto.response.ShoppingListResponse;
import com.nomnomnow.nnnbackend.dto.response.ShoppingListSummaryResponse;
import com.nomnomnow.nnnbackend.entity.RecipeComponent;
import com.nomnomnow.nnnbackend.entity.RecipePlan;
import com.nomnomnow.nnnbackend.entity.ShoppingList;
import com.nomnomnow.nnnbackend.entity.ShoppingListItem;
import com.nomnomnow.nnnbackend.entity.Unit;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.exception.ResourceNotFoundException;
import com.nomnomnow.nnnbackend.repository.ShoppingListRepository;
import com.nomnomnow.nnnbackend.user.AppUser;
import com.nomnomnow.nnnbackend.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.time.DayOfWeek.MONDAY;

@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListRepository shoppingListRepository;
    private final RecipePlanService recipePlanService;
    private final CurrentUserService currentUserService;

    @Transactional
    public ShoppingListResponse generateShoppingList(ShoppingListRequest request) {
        AppUser currentUser = currentUserService.getCurrentUser();
        var weekStart = request.weekStart().with(TemporalAdjusters.previousOrSame(MONDAY));
        var peopleCountsByDate = buildPeopleCountsByDate(request, weekStart);
        var plans = recipePlanService.getOrCreateWeeklyPlansForCurrentUser(weekStart);
        var aggregatedItems = aggregateItems(plans, peopleCountsByDate);

        if (aggregatedItems.isEmpty()) {
            throw new BadRequestException("Cannot generate a shopping list without recipe ingredients");
        }

        var shoppingList = new ShoppingList();
        shoppingList.setOwner(currentUser);
        shoppingList.setWeekStart(weekStart);
        aggregatedItems.forEach(shoppingList::addItem);

        return mapToResponse(shoppingListRepository.saveAndFlush(shoppingList));
    }

    @Transactional(readOnly = true)
    public List<ShoppingListSummaryResponse> getShoppingLists() {
        AppUser currentUser = currentUserService.getCurrentUser();

        return shoppingListRepository.findByOwnerOrderByCreatedAtDesc(currentUser).stream()
                .map(this::mapToSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShoppingListResponse getShoppingList(Long id) {
        AppUser currentUser = currentUserService.getCurrentUser();

        return shoppingListRepository.findByIdAndOwner(id, currentUser)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list not found with id: " + id));
    }

    @Transactional
    public void deleteShoppingList(Long id) {
        AppUser currentUser = currentUserService.getCurrentUser();

        var shoppingList = shoppingListRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list not found with id: " + id));

        shoppingListRepository.delete(shoppingList);
    }

    private Map<LocalDate, Integer> buildPeopleCountsByDate(ShoppingListRequest request, LocalDate weekStart) {
        if (request.days().isEmpty()) {
            return Map.of();
        }

        var weekEnd = weekStart.plusDays(6);
        var peopleCountsByDate = new HashMap<LocalDate, Integer>();

        for (var day : request.days()) {
            if (day == null || day.planDate() == null || day.peopleCount() == null || day.peopleCount() < 1) {
                throw new BadRequestException("People count must be at least 1 for each planned day");
            }

            if (day.planDate().isBefore(weekStart) || day.planDate().isAfter(weekEnd)) {
                throw new BadRequestException("People counts must belong to the requested week");
            }

            peopleCountsByDate.put(day.planDate(), day.peopleCount());
        }

        return peopleCountsByDate;
    }

    private List<ShoppingListItem> aggregateItems(List<RecipePlan> plans, Map<LocalDate, Integer> peopleCountsByDate) {
        var itemsByIngredientAndUnit = new LinkedHashMap<ShoppingListItemKey, ShoppingListItem>();

        for (RecipePlan plan : plans) {
            var peopleCount = peopleCountsByDate.getOrDefault(plan.getPlanDate(), 1);

            for (RecipeComponent component : plan.getRecipe().getComponents()) {
                if (component.getIngredient() == null || component.getQuantity() == null || component.getUnit() == null) {
                    continue;
                }

                var key = new ShoppingListItemKey(component.getIngredient().getName(), component.getUnit());
                var item = itemsByIngredientAndUnit.computeIfAbsent(key, ignored -> createItem(component));
                item.setQuantity(item.getQuantity().add(scaleQuantity(component, peopleCount)));
            }
        }

        return itemsByIngredientAndUnit.values().stream()
                .sorted(Comparator
                        .comparing(ShoppingListItem::getIngredientName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> item.getUnit().name()))
                .toList();
    }

    private ShoppingListItem createItem(RecipeComponent component) {
        var item = new ShoppingListItem();
        item.setIngredientName(component.getIngredient().getName());
        item.setUnit(component.getUnit());
        item.setQuantity(BigDecimal.ZERO);
        return item;
    }

    private BigDecimal scaleQuantity(RecipeComponent component, int peopleCount) {
        var recipeServings = component.getRecipe().getServings();
        var servings = recipeServings == null || recipeServings < 1 ? 1 : recipeServings;

        return component.getQuantity()
                .multiply(BigDecimal.valueOf(peopleCount))
                .divide(BigDecimal.valueOf(servings), 2, RoundingMode.HALF_UP);
    }

    private ShoppingListSummaryResponse mapToSummaryResponse(ShoppingList shoppingList) {
        return new ShoppingListSummaryResponse(
                shoppingList.getId(),
                shoppingList.getWeekStart(),
                shoppingList.getCreatedAt(),
                shoppingList.getItems().size()
        );
    }

    private ShoppingListResponse mapToResponse(ShoppingList shoppingList) {
        return new ShoppingListResponse(
                shoppingList.getId(),
                shoppingList.getWeekStart(),
                shoppingList.getCreatedAt(),
                shoppingList.getItems().stream()
                        .sorted(Comparator
                                .comparing(ShoppingListItem::getIngredientName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(item -> item.getUnit().name()))
                        .map(this::mapToItemResponse)
                        .toList()
        );
    }

    private ShoppingListItemResponse mapToItemResponse(ShoppingListItem item) {
        return new ShoppingListItemResponse(
                item.getIngredientName(),
                item.getQuantity(),
                item.getUnit()
        );
    }

    private record ShoppingListItemKey(String ingredientName, Unit unit) {
    }
}

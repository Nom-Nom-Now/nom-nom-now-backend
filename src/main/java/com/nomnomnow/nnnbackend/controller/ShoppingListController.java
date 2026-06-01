package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.dto.request.ShoppingListRequest;
import com.nomnomnow.nnnbackend.dto.response.ShoppingListResponse;
import com.nomnomnow.nnnbackend.dto.response.ShoppingListSummaryResponse;
import com.nomnomnow.nnnbackend.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    @PostMapping
    public ShoppingListResponse generateShoppingList(@Valid @RequestBody ShoppingListRequest request) {
        return shoppingListService.generateShoppingList(request);
    }

    @GetMapping
    public List<ShoppingListSummaryResponse> getShoppingLists() {
        return shoppingListService.getShoppingLists();
    }

    @GetMapping("/{id}")
    public ShoppingListResponse getShoppingList(@PathVariable Long id) {
        return shoppingListService.getShoppingList(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShoppingList(@PathVariable Long id) {
        shoppingListService.deleteShoppingList(id);
    }
}

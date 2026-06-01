package com.nomnomnow.nnnbackend.controller;

import com.nomnomnow.nnnbackend.exception.GlobalExceptionHandler;
import com.nomnomnow.nnnbackend.service.ShoppingListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShoppingListControllerTest {

    @Mock
    private ShoppingListService shoppingListService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ShoppingListController(shoppingListService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deleteShoppingListReturnsNoContentAndDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/shopping-lists/7"))
                .andExpect(status().isNoContent());

        verify(shoppingListService).deleteShoppingList(7L);
    }
}

package com.nomnomnow.nnnbackend.service;

import com.nomnomnow.nnnbackend.dto.request.CategoryRequest;
import com.nomnomnow.nnnbackend.entity.Category;
import com.nomnomnow.nnnbackend.exception.BadRequestException;
import com.nomnomnow.nnnbackend.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_whenRequestHasNoRecipes_savesTrimmedCategory() {
        var request = new CategoryRequest("  Breakfast ", "#ff0", List.of());
        var savedCategory = new Category();
        savedCategory.setId(42L);
        savedCategory.setName("Breakfast");
        savedCategory.setColor("#ff0");

        when(categoryRepository.findByNameIgnoreCase("Breakfast")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        var result = categoryService.create(request);

        var captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).findByNameIgnoreCase("Breakfast");
        verify(categoryRepository).save(captor.capture());

        var categoryToPersist = captor.getValue();
        assertThat(categoryToPersist.getName()).isEqualTo("Breakfast");
        assertThat(categoryToPersist.getColor()).isEqualTo("#ff0");

        assertThat(result).isSameAs(savedCategory);
    }

    @Test
    void create_whenCategoryAlreadyExists_throwsBadRequestException() {
        var request = new CategoryRequest("Lunch", "#123", List.of());
        when(categoryRepository.findByNameIgnoreCase("Lunch")).thenReturn(Optional.of(new Category()));

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Lunch");

        verify(categoryRepository, never()).save(any(Category.class));
    }
}

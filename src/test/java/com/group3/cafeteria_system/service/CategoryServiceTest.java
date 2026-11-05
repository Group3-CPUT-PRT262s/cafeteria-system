package com.group3.cafeteria_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.group3.cafeteria_system.model.Category;
import com.group3.cafeteria_system.repository.CategoryRepository;
import com.group3.cafeteria_system.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, menuItemRepository);
    }

    @Test
    void rejectsDuplicateCategoryNamesIgnoringCase() {
        when(categoryRepository.existsByCategoryNameIgnoreCase("Drinks")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.addCategory(" Drinks "));
    }

    @Test
    void refusesToDeleteCategoryThatStillHasMenuItems() {
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(new Category("Meals")));
        when(menuItemRepository.countByCategoryId(4L)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> categoryService.deleteCategory(4L));
    }

    @Test
    void updatesCategoryWhenNameIsUnique() {
        Category category = new Category("Old name");
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot("New name", 4L))
                .thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        assertEquals("New name", categoryService.updateCategory(4L, " New name ").getCategoryName());
        verify(categoryRepository).save(category);
    }
}

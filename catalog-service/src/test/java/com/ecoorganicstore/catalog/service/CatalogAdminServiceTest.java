package com.ecoorganicstore.catalog.service;

import com.ecoorganicstore.catalog.domain.Category;
import com.ecoorganicstore.catalog.domain.Product;
import com.ecoorganicstore.catalog.repo.CategoryRepository;
import com.ecoorganicstore.catalog.repo.ProductRepository;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.ProductWriteRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogAdminServiceTest {
    @Test
    void createProductDerivesSlugAndLeavesRatingUntouched() {
        ProductRepository products = mock(ProductRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CatalogAdminService service = new CatalogAdminService(products, categories);
        when(categories.findById("cat-1")).thenReturn(Optional.of(category("cat-1")));
        when(products.findBySlug("farm-carrots")).thenReturn(Optional.empty());
        when(products.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId("p-1");
            return saved;
        });

        var created = service.createProduct(new ProductWriteRequest(
                "Farm Carrots",
                " ",
                "Sweet carrots.",
                9900L,
                List.of(" https://images.example/carrot.jpg "),
                "cat-1",
                "Ooty",
                List.of("India Organic"),
                "500g",
                true,
                null));

        assertEquals("farm-carrots", created.slug());
        assertEquals(0, created.reviewCount());
        assertEquals(0d, created.averageRating());
        assertEquals(true, created.active());
        assertEquals(List.of("https://images.example/carrot.jpg"), created.images());
    }

    @Test
    void updateKeepsRatingAndRejectsASlugOwnedByAnotherProduct() {
        ProductRepository products = mock(ProductRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CatalogAdminService service = new CatalogAdminService(products, categories);
        Product current = new Product();
        current.setId("p-1");
        current.setAverageRating(4.5);
        current.setReviewCount(3);
        Product other = new Product();
        other.setId("p-2");
        when(products.findById("p-1")).thenReturn(Optional.of(current));
        when(categories.findById("cat-1")).thenReturn(Optional.of(category("cat-1")));
        when(products.findBySlug("wild-forest-honey")).thenReturn(Optional.of(other));

        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () -> service.updateProduct("p-1", write("Wild Honey", "wild-forest-honey")));

        assertEquals("A product with this slug already exists.", duplicate.getMessage());
        assertEquals(3, current.getReviewCount());
        verify(products, never()).save(any());
    }

    @Test
    void deleteCategoryRefusesWhileProductsRemain() {
        ProductRepository products = mock(ProductRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CatalogAdminService service = new CatalogAdminService(products, categories);
        when(categories.existsById("cat-1")).thenReturn(true);
        when(products.countByCategoryId("cat-1")).thenReturn(2L);

        IllegalArgumentException blocked = assertThrows(IllegalArgumentException.class, () -> service.deleteCategory("cat-1"));

        assertEquals("Move or remove the products in this category first.", blocked.getMessage());
        verify(categories, never()).deleteById("cat-1");
    }

    private static ProductWriteRequest write(String name, String slug) {
        return new ProductWriteRequest(name, slug, "Notes", 100L, List.of(), "cat-1", "Kerala", List.of(), "500g", false, true);
    }

    private static Category category(String id) {
        Category category = new Category();
        category.setId(id);
        category.setName("Produce");
        category.setSlug("produce");
        return category;
    }
}

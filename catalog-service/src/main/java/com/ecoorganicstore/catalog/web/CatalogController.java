package com.ecoorganicstore.catalog.web;

import com.ecoorganicstore.catalog.domain.Category;
import com.ecoorganicstore.catalog.domain.Product;
import com.ecoorganicstore.catalog.repo.CategoryRepository;
import com.ecoorganicstore.catalog.repo.ProductRepository;
import com.ecoorganicstore.catalog.service.CatalogAdminService;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.CategoryResponse;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.CategoryWriteRequest;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.ProductResponse;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.ProductWriteRequest;
import com.ecoorganicstore.common.security.AuthGuards;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class CatalogController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogAdminService catalogAdminService;

    public CatalogController(CategoryRepository categoryRepository, ProductRepository productRepository,
                             CatalogAdminService catalogAdminService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.catalogAdminService = catalogAdminService;
    }

    @GetMapping("/api/categories")
    public List<Category> categories() {
        return categoryRepository.findAll().stream().sorted((a,b)->Integer.compare(a.getSortOrder(), b.getSortOrder())).toList();
    }

    @GetMapping("/api/products")
    public List<Product> products(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false, defaultValue = "false") boolean featured) {
        if (featured) return productRepository.findByFeaturedTrueAndActiveTrue();
        if (search != null && !search.isBlank()) return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(search);
        if (category != null && !category.isBlank()) {
            var cat = categoryRepository.findBySlug(category).orElseThrow(() -> new IllegalArgumentException("Category not found"));
            return productRepository.findByCategoryIdAndActiveTrue(cat.getId());
        }
        return productRepository.findByActiveTrue();
    }

    @GetMapping("/api/products/{slug}")
    public Product product(@PathVariable String slug) {
        return productRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @GetMapping("/internal/products/{id}")
    public Product internalById(@PathVariable String id) {
        return productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @GetMapping("/internal/products")
    public List<ProductSnapshot> internalByIds(@RequestParam List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<String> distinct = ids.stream().filter(id -> id != null && !id.isBlank()).distinct().limit(40).toList();
        if (distinct.isEmpty()) return List.of();
        return productRepository.findAllById(distinct).stream().map(ProductSnapshot::from).toList();
    }

    @PatchMapping("/internal/products/{id}/rating")
    public Product updateRating(@PathVariable String id, @RequestBody RatingRequest request) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setAverageRating(request.averageRating());
        p.setReviewCount(request.reviewCount());
        return productRepository.save(p);
    }

    @PostMapping("/api/admin/catalog/products")
    public ProductResponse createProduct(HttpServletRequest request, @Valid @RequestBody ProductWriteRequest body) {
        ensureAdmin(request);
        return catalogAdminService.createProduct(body);
    }

    @PutMapping("/api/admin/catalog/products/{id}")
    public ProductResponse updateProduct(HttpServletRequest request, @PathVariable String id,
                                         @Valid @RequestBody ProductWriteRequest body) {
        ensureAdmin(request);
        return catalogAdminService.updateProduct(id, body);
    }

    @DeleteMapping("/api/admin/catalog/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(HttpServletRequest request, @PathVariable String id) {
        ensureAdmin(request);
        catalogAdminService.deleteProduct(id);
    }

    @GetMapping("/api/admin/catalog/products")
    public List<ProductResponse> adminProducts(HttpServletRequest request) {
        ensureAdmin(request);
        return catalogAdminService.products();
    }

    @GetMapping("/api/admin/catalog/categories")
    public List<CategoryResponse> adminCategories(HttpServletRequest request) {
        ensureAdmin(request);
        return catalogAdminService.categories();
    }

    @PostMapping("/api/admin/catalog/categories")
    public CategoryResponse createCategory(HttpServletRequest request, @Valid @RequestBody CategoryWriteRequest body) {
        ensureAdmin(request);
        return catalogAdminService.createCategory(body);
    }

    @PutMapping("/api/admin/catalog/categories/{id}")
    public CategoryResponse updateCategory(HttpServletRequest request, @PathVariable String id,
                                           @Valid @RequestBody CategoryWriteRequest body) {
        ensureAdmin(request);
        return catalogAdminService.updateCategory(id, body);
    }

    @DeleteMapping("/api/admin/catalog/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(HttpServletRequest request, @PathVariable String id) {
        ensureAdmin(request);
        catalogAdminService.deleteCategory(id);
    }

    private static void ensureAdmin(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
    }

    public record RatingRequest(double averageRating, long reviewCount) {}
}
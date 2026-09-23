package com.ecoorganicstore.catalog.web;

import com.ecoorganicstore.catalog.domain.Category;
import com.ecoorganicstore.catalog.domain.Product;
import com.ecoorganicstore.catalog.repo.CategoryRepository;
import com.ecoorganicstore.catalog.repo.ProductRepository;
import com.ecoorganicstore.common.security.AuthGuards;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class CatalogController {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CatalogController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
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

    @PatchMapping("/internal/products/{id}/rating")
    public Product updateRating(@PathVariable String id, @RequestBody RatingRequest request) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setAverageRating(request.averageRating());
        p.setReviewCount(request.reviewCount());
        return productRepository.save(p);
    }

    @PostMapping("/api/admin/catalog/products")
    public Product createProduct(HttpServletRequest request, @RequestBody Product product) {
        ensureAdmin(request);
        return productRepository.save(product);
    }

    @PutMapping("/api/admin/catalog/products/{id}")
    public Product updateProduct(HttpServletRequest request, @PathVariable String id, @RequestBody Product req) {
        ensureAdmin(request);
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setName(req.getName()); p.setDescription(req.getDescription()); p.setPricePaise(req.getPricePaise());
        p.setImages(req.getImages()); p.setCategoryId(req.getCategoryId()); p.setOrigin(req.getOrigin());
        p.setCertifications(req.getCertifications()); p.setUnit(req.getUnit()); p.setFeatured(req.isFeatured()); p.setActive(req.isActive());
        return productRepository.save(p);
    }

    @DeleteMapping("/api/admin/catalog/products/{id}")
    public void deleteProduct(HttpServletRequest request, @PathVariable String id) {
        ensureAdmin(request);
        productRepository.deleteById(id);
    }

    @GetMapping("/api/admin/catalog/products")
    public List<Product> adminProducts(HttpServletRequest request) {
        ensureAdmin(request);
        return productRepository.findAll();
    }

    @GetMapping("/api/admin/catalog/categories")
    public List<Category> adminCategories(HttpServletRequest request) {
        ensureAdmin(request);
        return categoryRepository.findAll();
    }

    @PostMapping("/api/admin/catalog/categories")
    public Category createCategory(HttpServletRequest request, @RequestBody Category category) {
        ensureAdmin(request);
        return categoryRepository.save(category);
    }

    @PutMapping("/api/admin/catalog/categories/{id}")
    public Category updateCategory(HttpServletRequest request, @PathVariable String id, @RequestBody Category requestCategory) {
        ensureAdmin(request);
        Category category = categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setName(requestCategory.getName());
        category.setSlug(requestCategory.getSlug());
        category.setImage(requestCategory.getImage());
        category.setSortOrder(requestCategory.getSortOrder());
        return categoryRepository.save(category);
    }

    @DeleteMapping("/api/admin/catalog/categories/{id}")
    public void deleteCategory(HttpServletRequest request, @PathVariable String id) {
        ensureAdmin(request);
        categoryRepository.deleteById(id);
    }

    private static void ensureAdmin(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
    }

    public record RatingRequest(double averageRating, long reviewCount) {}
}
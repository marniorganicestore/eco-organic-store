package com.ecoorganicstore.catalog.service;

import com.ecoorganicstore.catalog.domain.Category;
import com.ecoorganicstore.catalog.domain.Product;
import com.ecoorganicstore.catalog.repo.CategoryRepository;
import com.ecoorganicstore.catalog.repo.ProductRepository;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.CategoryResponse;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.CategoryWriteRequest;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.ProductResponse;
import com.ecoorganicstore.catalog.web.CatalogAdminDtos.ProductWriteRequest;
import com.ecoorganicstore.common.web.PageResponse;
import com.ecoorganicstore.common.web.PageWindow;
import com.ecoorganicstore.common.web.SearchText;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CatalogAdminService {
    private static final int MAX_IMAGES = 6;
    private static final int MAX_CERTIFICATIONS = 8;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CatalogAdminService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public PageResponse<ProductResponse> products(int page, int size, String query) {
        int safePage = PageWindow.page(page);
        int safeSize = PageWindow.size(size, PageWindow.ADMIN_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
        String pattern = SearchText.literal(query);
        Page<Product> result = pattern.isEmpty()
                ? productRepository.findAll(pageable)
                : productRepository.searchByText(pattern, pageable);
        List<ProductResponse> items = result.getContent().stream().map(CatalogAdminService::toResponse).toList();
        return PageResponse.of(items, safePage, safeSize, result.getTotalElements());
    }

    public List<ProductResponse> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<String> distinct = ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .limit(PageWindow.MAX_SIZE)
                .toList();
        if (distinct.isEmpty()) return List.of();
        return productRepository.findAllById(distinct).stream().map(CatalogAdminService::toResponse).toList();
    }

    public CatalogDeskSummary summary() {
        return new CatalogDeskSummary(productRepository.count(), categoryRepository.count());
    }

    public record CatalogDeskSummary(long productCount, long categoryCount) {}

    public ProductResponse createProduct(ProductWriteRequest request) {
        String name = request.name().trim();
        String slug = slugOf(request.slug(), name);
        ensureProductSlugFree(slug, null);
        String categoryId = requireCategory(request.categoryId());
        Product product = new Product();
        apply(product, request, name, slug, categoryId);
        return toResponse(saveUnique(() -> productRepository.save(product)));
    }

    public ProductResponse updateProduct(String id, ProductWriteRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        String name = request.name().trim();
        String slug = slugOf(request.slug(), name);
        ensureProductSlugFree(slug, product.getId());
        String categoryId = requireCategory(request.categoryId());
        apply(product, request, name, slug, categoryId);
        return toResponse(saveUnique(() -> productRepository.save(product)));
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found.");
        }
        productRepository.deleteById(id);
    }

    public List<CategoryResponse> categories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparingInt((Category category) -> category.getSortOrder() == null ? 0 : category.getSortOrder())
                        .thenComparing(Category::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(CatalogAdminService::toResponse)
                .toList();
    }

    public CategoryResponse createCategory(CategoryWriteRequest request) {
        String name = request.name().trim();
        String slug = slugOf(request.slug(), name);
        ensureCategorySlugFree(slug, null);
        Category category = new Category();
        apply(category, request, name, slug);
        return toResponse(saveUnique(() -> categoryRepository.save(category)));
    }

    public CategoryResponse updateCategory(String id, CategoryWriteRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Category not found."));
        String name = request.name().trim();
        String slug = slugOf(request.slug(), name);
        ensureCategorySlugFree(slug, category.getId());
        apply(category, request, name, slug);
        return toResponse(saveUnique(() -> categoryRepository.save(category)));
    }

    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found.");
        }
        if (productRepository.countByCategoryId(id) > 0) {
            throw new IllegalArgumentException("Move or remove the products in this category first.");
        }
        categoryRepository.deleteById(id);
    }

    private void apply(Product product, ProductWriteRequest request, String name, String slug, String categoryId) {
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(blankToEmpty(request.description()));
        product.setPricePaise(request.pricePaise());
        product.setImages(cleanList(request.images(), "Images", MAX_IMAGES));
        product.setCategoryId(categoryId);
        product.setOrigin(blankToEmpty(request.origin()));
        product.setCertifications(cleanList(request.certifications(), "Certifications", MAX_CERTIFICATIONS));
        product.setUnit(request.unit().trim());
        product.setFeatured(Boolean.TRUE.equals(request.featured()));
        product.setActive(request.active() == null || request.active());
    }

    private void apply(Category category, CategoryWriteRequest request, String name, String slug) {
        category.setName(name);
        category.setSlug(slug);
        category.setImage(blankToEmpty(request.image()));
        category.setSortOrder(request.sortOrder());
    }

    private String requireCategory(String categoryId) {
        String id = categoryId.trim();
        if (categoryRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Category not found.");
        }
        return id;
    }

    private void ensureProductSlugFree(String slug, String currentId) {
        productRepository.findBySlug(slug).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId())) {
                throw new IllegalArgumentException("A product with this slug already exists.");
            }
        });
    }

    private void ensureCategorySlugFree(String slug, String currentId) {
        categoryRepository.findBySlug(slug).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId())) {
                throw new IllegalArgumentException("A category with this slug already exists.");
            }
        });
    }

    private static String slugOf(String requested, String name) {
        String source = requested == null || requested.isBlank() ? name : requested;
        String slug = source.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank() || slug.length() > 140) {
            throw new IllegalArgumentException("Use a short name made of letters or numbers.");
        }
        return slug;
    }

    private static List<String> cleanList(List<String> values, String label, int max) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (cleaned.size() > max) {
            throw new IllegalArgumentException(label + " can include at most " + max + " entries.");
        }
        for (String value : cleaned) {
            if (value.length() > 500) {
                throw new IllegalArgumentException(label + " entries are too long.");
            }
        }
        return cleaned;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static <T> T saveUnique(Supplier<T> save) {
        try {
            return save.get();
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("That slug is already in use.");
        }
    }

    private static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                product.getPricePaise(),
                product.getImages() == null ? List.of() : List.copyOf(product.getImages()),
                product.getCategoryId(),
                product.getOrigin(),
                product.getCertifications() == null ? List.of() : List.copyOf(product.getCertifications()),
                product.getUnit(),
                product.isActive(),
                product.isFeatured(),
                product.getAverageRating(),
                product.getReviewCount());
    }

    private static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                category.getImage(),
                category.getSortOrder() == null ? 0 : category.getSortOrder());
    }
}

package com.ecoorganicstore.catalog.service;

import com.ecoorganicstore.catalog.domain.Category;
import com.ecoorganicstore.catalog.domain.Product;
import com.ecoorganicstore.catalog.repo.CategoryRepository;
import com.ecoorganicstore.catalog.repo.ProductRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements CommandLineRunner {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public SeedData(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        Category produce = createCategory("produce", "Produce", 1);
        Category pantry = createCategory("pantry", "Pantry", 2);
        Category dairy = createCategory("dairy", "Dairy", 3);
        createProduct("organic-baby-spinach", "Organic Baby Spinach", 17900, produce.getId(), "Mysuru", true);
        createProduct("farm-carrots", "Farm Fresh Carrots", 9900, produce.getId(), "Ooty", true);
        createProduct("cold-pressed-coconut-oil", "Cold Pressed Coconut Oil", 45900, pantry.getId(), "Kerala", false);
        createProduct("wild-forest-honey", "Wild Forest Honey", 32900, pantry.getId(), "Coorg", true);
        createProduct("a2-bilona-ghee", "A2 Bilona Ghee", 79900, dairy.getId(), "Anand", false);
        createProduct("organic-curd", "Organic Curd", 11900, dairy.getId(), "Pune", false);
    }

    private Category createCategory(String slug, String name, int order) {
        Category c = new Category();
        c.setSlug(slug);
        c.setName(name);
        c.setImage("https://images.unsplash.com/photo-1542838132-92c53300491e");
        c.setSortOrder(order);
        return categoryRepository.save(c);
    }

    private void createProduct(String slug, String name, long pricePaise, String categoryId, String origin, boolean featured) {
        Product p = new Product();
        p.setSlug(slug);
        p.setName(name);
        p.setDescription(name + " sourced from trusted organic farms.");
        p.setPricePaise(pricePaise);
        p.setCategoryId(categoryId);
        p.setImages(List.of("https://images.unsplash.com/photo-1540420773420-3366772f4999"));
        p.setOrigin(origin);
        p.setCertifications(List.of("India Organic"));
        p.setUnit("500g");
        p.setFeatured(featured);
        productRepository.save(p);
    }
}
package com.ecoorganicstore.catalog.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class CatalogAdminDtos {
    private CatalogAdminDtos() {}

    public record ProductWriteRequest(
            @NotBlank(message = "Enter a product name.") @Size(max = 120) String name,
            @Size(max = 140) String slug,
            @Size(max = 2000) String description,
            @NotNull(message = "Enter a price.") @Min(value = 0, message = "Price cannot be negative.") Long pricePaise,
            List<String> images,
            @NotBlank(message = "Choose a category.") String categoryId,
            @Size(max = 80) String origin,
            List<String> certifications,
            @NotBlank(message = "Enter a unit, such as 500g.") @Size(max = 40) String unit,
            Boolean featured,
            Boolean active) {}

    public record ProductResponse(
            String id,
            String slug,
            String name,
            String description,
            long pricePaise,
            List<String> images,
            String categoryId,
            String origin,
            List<String> certifications,
            String unit,
            boolean active,
            boolean featured,
            double averageRating,
            long reviewCount) {}

    public record CategoryWriteRequest(
            @NotBlank(message = "Enter a category name.") @Size(max = 80) String name,
            @Size(max = 80) String slug,
            @Size(max = 500) String image,
            @NotNull(message = "Enter a sort order.") @Min(value = 0, message = "Sort order cannot be negative.")
            @Max(value = 999, message = "Sort order must be 999 or less.") Integer sortOrder) {}

    public record CategoryResponse(String id, String slug, String name, String image, int sortOrder) {}
}

package com.ecoorganicstore.inventory.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.inventory.domain.Reservation;
import com.ecoorganicstore.inventory.domain.Stock;
import com.ecoorganicstore.inventory.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/internal/inventory/reserve")
    public Reservation reserve(@RequestBody ReserveRequest request) {
        return inventoryService.reserve(request.orderId(), request.lines());
    }

    @PostMapping("/internal/inventory/confirm/{orderId}")
    public Reservation confirm(@PathVariable String orderId) {
        return inventoryService.confirm(orderId);
    }

    @PostMapping("/internal/inventory/release/{orderId}")
    public Reservation release(@PathVariable String orderId) {
        return inventoryService.release(orderId);
    }

    @GetMapping("/internal/stock")
    public List<StockView> stock(@RequestParam List<String> productIds) {
        return inventoryService.stockByProducts(productIds).stream().map(s -> new StockView(s.getProductId(), s.available())).toList();
    }

    @GetMapping("/api/admin/inventory")
    public List<AdminStockResponse> inventory(HttpServletRequest request) {
        ensureAdmin(request);
        return inventoryService.list().stream().map(InventoryController::toAdmin).toList();
    }

    @PatchMapping("/api/admin/inventory/{productId}")
    public AdminStockResponse adjust(HttpServletRequest request, @PathVariable String productId,
                                     @Valid @RequestBody AdjustRequest adjustRequest) {
        ensureAdmin(request);
        return toAdmin(inventoryService.adjust(productId, adjustRequest.onHand()));
    }

    @GetMapping("/api/admin/inventory/low-stock")
    public List<StockView> lowStock(HttpServletRequest request, @RequestParam(defaultValue = "10") int threshold) {
        ensureAdmin(request);
        return inventoryService.getLowStock(threshold).stream().map(s -> new StockView(s.getProductId(), s.available())).toList();
    }

    private static AdminStockResponse toAdmin(Stock stock) {
        return new AdminStockResponse(stock.getProductId(), stock.getOnHand(), stock.getReserved(), stock.available());
    }

    private static void ensureAdmin(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
    }

    public record ReserveRequest(String orderId, List<Reservation.Line> lines) {}
    public record AdjustRequest(
            @NotNull(message = "Enter the on-hand quantity.")
            @Min(value = 0, message = "On-hand quantity cannot be negative.") Integer onHand) {}
    public record StockView(String productId, int available) {}
    public record AdminStockResponse(String productId, int onHand, int reserved, int available) {}
}
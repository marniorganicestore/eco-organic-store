package com.harvest.inventory.web;

import com.harvest.common.security.AuthGuards;
import com.harvest.inventory.domain.Reservation;
import com.harvest.inventory.domain.Stock;
import com.harvest.inventory.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PatchMapping("/api/admin/inventory/{productId}")
    public Stock adjust(HttpServletRequest request, @PathVariable String productId, @RequestBody AdjustRequest adjustRequest) {
        ensureAdmin(request);
        return inventoryService.adjust(productId, adjustRequest.onHand());
    }

    @GetMapping("/api/admin/inventory/low-stock")
    public List<StockView> lowStock(HttpServletRequest request, @RequestParam(defaultValue = "10") int threshold) {
        ensureAdmin(request);
        return inventoryService.getLowStock(threshold).stream().map(s -> new StockView(s.getProductId(), s.available())).toList();
    }

    private static void ensureAdmin(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
    }

    public record ReserveRequest(String orderId, List<Reservation.Line> lines) {}
    public record AdjustRequest(int onHand) {}
    public record StockView(String productId, int available) {}
}
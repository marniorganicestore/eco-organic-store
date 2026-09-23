package com.ecoorganicstore.inventory.service;

import com.ecoorganicstore.inventory.domain.Reservation;
import com.ecoorganicstore.inventory.domain.Stock;
import com.ecoorganicstore.inventory.repo.ReservationRepository;
import com.ecoorganicstore.inventory.repo.StockRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    public Reservation reserve(String orderId, List<Reservation.Line> lines) {
        for (var line : lines) {
            Stock stock = stockRepository.findByProductId(line.productId()).orElseGet(() -> {
                Stock s = new Stock();
                s.setProductId(line.productId());
                s.setOnHand(0);
                s.setReserved(0);
                return s;
            });
            if (stock.available() < line.qty()) {
                throw new IllegalArgumentException("Insufficient stock for " + line.productId());
            }
            stock.setReserved(stock.getReserved() + line.qty());
            stockRepository.save(stock);
        }
        Reservation reservation = new Reservation();
        reservation.setOrderId(orderId == null || orderId.isBlank() ? UUID.randomUUID().toString() : orderId);
        reservation.setLines(new ArrayList<>(lines));
        reservation.setExpiresAt(Instant.now().plusSeconds(900));
        reservation.setStatus("PENDING");
        return reservationRepository.save(reservation);
    }

    public Reservation confirm(String orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (!"PENDING".equals(reservation.getStatus())) return reservation;
        for (var line : reservation.getLines()) {
            Stock stock = stockRepository.findByProductId(line.productId()).orElseThrow(() -> new IllegalArgumentException("Stock missing"));
            stock.setReserved(Math.max(0, stock.getReserved() - line.qty()));
            stock.setOnHand(Math.max(0, stock.getOnHand() - line.qty()));
            stockRepository.save(stock);
        }
        reservation.setStatus("CONFIRMED");
        return reservationRepository.save(reservation);
    }

    public Reservation release(String orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (!"PENDING".equals(reservation.getStatus())) return reservation;
        for (var line : reservation.getLines()) {
            Stock stock = stockRepository.findByProductId(line.productId()).orElseThrow(() -> new IllegalArgumentException("Stock missing"));
            stock.setReserved(Math.max(0, stock.getReserved() - line.qty()));
            stockRepository.save(stock);
        }
        reservation.setStatus("RELEASED");
        return reservationRepository.save(reservation);
    }

    public Stock adjust(String productId, int onHand) {
        if (onHand < 0) {
            throw new IllegalArgumentException("On-hand quantity cannot be negative.");
        }
        Stock stock = stockRepository.findByProductId(productId).orElseGet(Stock::new);
        stock.setProductId(productId);
        stock.setOnHand(onHand);
        if (stock.getReserved() > onHand) {
            stock.setReserved(onHand);
        }
        return stockRepository.save(stock);
    }

    public List<Stock> list() {
        return stockRepository.findAll();
    }

    public List<Stock> getLowStock(int threshold) {
        return stockRepository.findAll().stream().filter(s -> s.available() <= threshold).toList();
    }

    public List<Stock> stockByProducts(List<String> productIds) {
        return stockRepository.findByProductIdIn(productIds);
    }
}
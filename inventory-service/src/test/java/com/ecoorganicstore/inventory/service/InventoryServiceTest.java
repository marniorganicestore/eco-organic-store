package com.ecoorganicstore.inventory.service;

import com.ecoorganicstore.inventory.domain.Stock;
import com.ecoorganicstore.inventory.repo.ReservationRepository;
import com.ecoorganicstore.inventory.repo.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceTest {
    @Test
    void adjustRejectsNegativeOnHand() {
        InventoryService service = new InventoryService(mock(StockRepository.class), mock(ReservationRepository.class));

        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class, () -> service.adjust("p1", -1));

        assertEquals("On-hand quantity cannot be negative.", rejected.getMessage());
    }

    @Test
    void adjustClampsReservedWhenOnHandDropsBelowTheHold() {
        StockRepository stocks = mock(StockRepository.class);
        Stock stock = new Stock();
        stock.setProductId("p1");
        stock.setOnHand(10);
        stock.setReserved(4);
        when(stocks.findByProductId("p1")).thenReturn(Optional.of(stock));
        when(stocks.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InventoryService service = new InventoryService(stocks, mock(ReservationRepository.class));

        Stock saved = service.adjust("p1", 2);

        assertEquals(2, saved.getOnHand());
        assertEquals(2, saved.getReserved());
        assertEquals(0, saved.available());
    }
}

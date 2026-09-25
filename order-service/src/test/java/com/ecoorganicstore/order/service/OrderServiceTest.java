package com.ecoorganicstore.order.service;

import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.repo.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private OrderRepository repository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        service = new OrderService(repository, mock(RestClient.class), "internal-secret", mock(OrderNotifier.class));
    }

    @Test
    void ordersByUserReadsOnlyTheRequestedPageForThatCustomer() {
        Order newer = order("HC-NEW", Instant.parse("2026-06-01T00:00:00Z"));
        when(repository.findByUserId(eq("user-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newer), org.springframework.data.domain.PageRequest.of(0, 10), 40));

        var page = service.ordersByUser("user-1", 0, 10);

        assertEquals(List.of("HC-NEW"), page.getContent().stream().map(Order::getOrderNumber).toList());
        assertEquals(40, page.getTotalElements());
        assertEquals(10, page.getSize());
    }

    @Test
    void deskUsesTheKolkataDayAndSumsThoseOrders() {
        Instant now = Instant.parse("2026-09-25T13:00:00Z");
        Order morning = order("HC-1", Instant.parse("2026-09-25T04:00:00Z"), 15_000);
        Order afternoon = order("HC-2", Instant.parse("2026-09-25T12:00:00Z"), 25_000);
        when(repository.findCreatedFromInclusiveUntil(
                Instant.parse("2026-09-24T18:30:00Z"),
                Instant.parse("2026-09-25T18:30:00Z")))
                .thenReturn(List.of(afternoon, morning));
        when(repository.findByOrderStatus(eq("CONFIRMED"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(morning), PageRequest.of(0, 5), 8));

        OrderService.OrderDesk desk = service.desk(now);

        assertEquals(2, desk.todayCount());
        assertEquals(40_000, desk.todayTotalPaise());
        assertEquals(8, desk.confirmedCount());
        assertEquals(List.of("HC-1"), desk.confirmed().stream().map(Order::getOrderNumber).toList());
        assertEquals(List.of("HC-2", "HC-1"), desk.today().stream().map(Order::getOrderNumber).toList());
    }

    private static Order order(String number, Instant createdAt) {
        return order(number, createdAt, 0);
    }

    private static Order order(String number, Instant createdAt, long totalPaise) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setUserId("user-1");
        order.setCreatedAt(createdAt);
        order.setTotalPaise(totalPaise);
        return order;
    }
}

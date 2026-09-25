package com.ecoorganicstore.order.service;

import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.repo.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
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

    private static Order order(String number, Instant createdAt) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setUserId("user-1");
        order.setCreatedAt(createdAt);
        return order;
    }
}

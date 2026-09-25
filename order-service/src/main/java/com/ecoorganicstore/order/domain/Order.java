package com.ecoorganicstore.order.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orders")
@CompoundIndex(name = "created_idx", def = "{'createdAt':-1,'_id':-1}")
@CompoundIndex(name = "user_created_idx", def = "{'userId':1,'createdAt':-1,'_id':-1}")
@CompoundIndex(name = "status_created_idx", def = "{'orderStatus':1,'createdAt':-1,'_id':-1}")
public class Order {
    @Id private String id;
    @Indexed(unique = true) private String orderNumber;
    private String userId;
    private List<Line> lines;
    private String shippingAddress;
    private long totalPaise;
    private String orderStatus;
    private String paymentId;
    private String reservationId;
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public long getTotalPaise() { return totalPaise; }
    public void setTotalPaise(long totalPaise) { this.totalPaise = totalPaise; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public record Line(String productId, String productName, long pricePaise, int qty, String image) {}
}
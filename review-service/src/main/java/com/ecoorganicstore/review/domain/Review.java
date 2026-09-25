package com.ecoorganicstore.review.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("reviews")
@CompoundIndex(name = "user_product_unique", def = "{'userId':1,'productId':1}", unique = true)
@CompoundIndex(name = "product_status_created_idx", def = "{'productId':1,'status':1,'createdAt':-1}")
@CompoundIndex(name = "status_created_idx", def = "{'status':1,'createdAt':-1}")
public class Review {
    @Id private String id;
    private String userId;
    private String productId;
    private int rating;
    private String body;
    private boolean verifiedPurchase;
    private String status = "VISIBLE";
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isVerifiedPurchase() { return verifiedPurchase; }
    public void setVerifiedPurchase(boolean verifiedPurchase) { this.verifiedPurchase = verifiedPurchase; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
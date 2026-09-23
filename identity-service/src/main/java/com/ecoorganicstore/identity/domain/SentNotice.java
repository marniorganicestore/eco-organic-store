package com.ecoorganicstore.identity.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sent_notices")
public class SentNotice {
    @Id
    private String id;
    @Indexed(unique = true)
    private String dedupeKey;
    private Instant sentAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}

package com.ecoorganicstore.identity.repo;

import com.ecoorganicstore.identity.domain.SentNotice;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SentNoticeRepository extends MongoRepository<SentNotice, String> {
}

package com.ecoorganicstore.catalog.repo;

import com.ecoorganicstore.catalog.domain.MediaAsset;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaRepository extends MongoRepository<MediaAsset, String> {
}

package com.ecoorganicstore.identity.repo;

import com.ecoorganicstore.identity.domain.AvatarAsset;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AvatarRepository extends MongoRepository<AvatarAsset, String> {
}

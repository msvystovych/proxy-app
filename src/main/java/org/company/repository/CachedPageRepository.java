package org.company.repository;

import org.company.model.CachedPage;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CachedPageRepository extends ReactiveMongoRepository<CachedPage, String> {
}
package org.company.repository;

import org.company.model.RequestResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestResponseRepository extends JpaRepository<RequestResponseEntity, Long> {
}
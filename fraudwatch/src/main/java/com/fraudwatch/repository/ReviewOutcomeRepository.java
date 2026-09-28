package com.fraudwatch.repository;

import com.fraudwatch.entity.ReviewOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewOutcomeRepository extends JpaRepository<ReviewOutcome, Long> {
    Optional<ReviewOutcome> findByTransactionId(Long transactionId);
    boolean existsByTransactionId(Long transactionId);
}

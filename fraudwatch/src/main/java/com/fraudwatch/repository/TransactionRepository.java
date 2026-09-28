package com.fraudwatch.repository;

import com.fraudwatch.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findAllByOrderByTimestampDesc();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sender.id = :senderId AND t.timestamp >= :since")
    long countRecentTransactionsBySender(@Param("senderId") Long senderId, @Param("since") LocalDateTime since);

    long countByStatus(Transaction.TransactionStatus status);
}

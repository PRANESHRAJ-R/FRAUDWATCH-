package com.fraudwatch.repository;

import com.fraudwatch.entity.FlaggedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlaggedTransactionRepository extends JpaRepository<FlaggedTransaction, Long> {

    List<FlaggedTransaction> findByTransactionId(Long transactionId);

    List<FlaggedTransaction> findByResolvedFalse();

    List<FlaggedTransaction> findByResolvedFalseOrderByFlaggedAtDesc();

    long countByRuleCode(String ruleCode);

    @Query("SELECT f.rule.code, COUNT(f) FROM FlaggedTransaction f GROUP BY f.rule.code")
    List<Object[]> countGroupedByRuleCode();
}

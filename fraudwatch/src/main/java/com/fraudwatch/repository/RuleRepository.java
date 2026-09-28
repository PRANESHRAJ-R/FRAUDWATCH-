package com.fraudwatch.repository;

import com.fraudwatch.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByEnabledTrue();
    Optional<Rule> findByCode(String code);
    boolean existsByCode(String code);
}

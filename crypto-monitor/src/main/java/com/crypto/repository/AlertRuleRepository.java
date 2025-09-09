// ===== AlertRuleRepository.java =====
// Localização: src/main/java/com/crypto/repository/AlertRuleRepository.java
package com.crypto.repository;

import com.crypto.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByCoinSymbolAndIsActiveTrue(String coinSymbol);

    List<AlertRule> findByIsActiveTrue();

    List<AlertRule> findByNotificationEmail(String email);
}
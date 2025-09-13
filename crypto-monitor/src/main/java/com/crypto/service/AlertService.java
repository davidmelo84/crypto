package com.crypto.service;

import com.crypto.model.AlertRule;
import com.crypto.dto.CryptoCurrency;
import com.crypto.model.User;
import com.crypto.model.dto.NotificationMessage;
import com.crypto.repository.AlertRuleRepository;
import com.crypto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public void processAlerts(List<CryptoCurrency> cryptos) {
        for (CryptoCurrency crypto : cryptos) {
            checkAlertsForCrypto(crypto);
        }
    }

    public void checkAlertsForCrypto(CryptoCurrency crypto) {
        List<AlertRule> rules = alertRuleRepository.findByCoinSymbolAndActiveTrue(crypto.getSymbol());

        for (AlertRule rule : rules) {
            try {
                if (shouldTriggerAlert(crypto, rule)) {
                    triggerAlert(crypto, rule);
                }
            } catch (Exception e) {
                log.error("Erro ao verificar regra {} para {}: {}", rule.getId(), crypto.getSymbol(), e.getMessage());
            }
        }
    }

    private boolean shouldTriggerAlert(CryptoCurrency crypto, AlertRule rule) {
        BigDecimal threshold = rule.getThresholdValue();

        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return crypto.getCurrentPrice() != null && crypto.getCurrentPrice().compareTo(threshold) >= 0;

            case PRICE_DECREASE:
                return crypto.getCurrentPrice() != null && crypto.getCurrentPrice().compareTo(threshold) <= 0;

            case VOLUME_SPIKE:
                return crypto.getTotalVolume() != null && crypto.getTotalVolume().compareTo(threshold) >= 0;

            case PERCENT_CHANGE_24H:
                return crypto.getPriceChange24h() != null &&
                        BigDecimal.valueOf(Math.abs(crypto.getPriceChange24h())).compareTo(threshold) >= 0;

            case MARKET_CAP:
                return crypto.getMarketCap() != null && crypto.getMarketCap().compareTo(threshold) >= 0;

            default:
                return false;
        }
    }

    private void triggerAlert(CryptoCurrency crypto, AlertRule rule) {
        String message = buildAlertMessage(crypto, rule);

        NotificationMessage notification = NotificationMessage.builder()
                .coinSymbol(crypto.getSymbol())
                .coinName(crypto.getName())
                .currentPrice("$" + df.format(crypto.getCurrentPrice()))
                .changePercentage(crypto.getPriceChange24h() != null
                        ? String.format("%.2f%%", crypto.getPriceChange24h())
                        : "N/A")
                .alertType(rule.getAlertType())
                .message(message)
                .recipient(rule.getNotificationEmail())
                .build();

        notificationService.sendNotification(notification);

        log.info("Alerta disparado: {} - {} -> {}", crypto.getSymbol(), rule.getAlertType(), message);
    }

    private String buildAlertMessage(CryptoCurrency crypto, AlertRule rule) {
        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return String.format(
                        "\uD83D\uDE80 %s (%s) atingiu $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(crypto.getCurrentPrice()),
                        df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case PRICE_DECREASE:
                return String.format(
                        "\uD83D\uDCC9 %s (%s) caiu para $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(crypto.getCurrentPrice()),
                        df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case VOLUME_SPIKE:
                return String.format(
                        "\uD83D\uDCCA %s (%s) com volume acima de %s (atual %s)",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(rule.getThresholdValue()),
                        df.format(crypto.getTotalVolume())
                );

            case PERCENT_CHANGE_24H:
                return String.format(
                        "\u26A1 %s (%s) variou %.2f%% nas últimas 24h (limite: %s%%)",
                        crypto.getName(),
                        crypto.getSymbol(),
                        crypto.getPriceChange24h(),
                        df.format(rule.getThresholdValue())
                );

            case MARKET_CAP:
                return String.format(
                        "\uD83C\uDFE6 %s (%s) com market cap acima de %s (atual %s)",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(rule.getThresholdValue()),
                        df.format(crypto.getMarketCap())
                );

            default:
                return String.format("%s (%s) - alerta disparado", crypto.getName(), crypto.getSymbol());
        }
    }

    public AlertRule createAlertRule(AlertRule alertRule) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                    : null;

            if (principal instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                Optional<User> u = userRepository.findByUsername(username);
                u.ifPresent(alertRule::setUser);
            }
        } catch (Exception e) {
            log.debug("Não foi possível vincular usuário à regra: {}", e.getMessage());
        }

        alertRule.setActive(true);
        AlertRule saved = alertRuleRepository.save(alertRule);
        log.info("Nova regra de alerta criada: {}", saved);
        return saved;
    }

    public List<AlertRule> getActiveAlertRules() {
        return alertRuleRepository.findByActiveTrue();
    }

    public void deactivateAlertRule(Long ruleId) {
        alertRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setActive(false);
            alertRuleRepository.save(rule);
            log.info("Regra de alerta {} desativada", ruleId);
        });
    }

    public java.util.List<AlertRule> getAlertRulesForUser(String username) {
        return alertRuleRepository.findAll()
                .stream()
                .filter(r -> r.getUser() != null && username.equals(r.getUser().getUsername()))
                .toList();
    }

}
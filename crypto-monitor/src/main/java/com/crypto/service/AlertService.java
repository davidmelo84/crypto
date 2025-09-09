package com.crypto.service;

import com.crypto.model.AlertRule;
import com.crypto.model.CryptoCurrency;
import com.crypto.model.dto.NotificationMessage;
import com.crypto.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationService notificationService;

    @Value("${alert.buy.threshold:-5.0}")
    private Double buyThreshold;

    @Value("${alert.sell.threshold:10.0}")
    private Double sellThreshold;

    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    /**
     * Verifica alertas para uma criptomoeda específica
     */
    public void checkAlertsForCrypto(CryptoCurrency crypto) {
        try {
            // Verificar alertas automáticos (regras padrão)
            checkDefaultAlerts(crypto);

            // Verificar regras de alertas personalizadas
            List<AlertRule> activeRules = alertRuleRepository
                    .findByCoinSymbolAndIsActiveTrue(crypto.getSymbol().toUpperCase());

            for (AlertRule rule : activeRules) {
                checkCustomAlert(crypto, rule);
            }

        } catch (Exception e) {
            log.error("Erro ao verificar alertas para {}: {}", crypto.getSymbol(), e.getMessage());
        }
    }

    /**
     * Verifica alertas padrão baseados em thresholds configurados
     */
    private void checkDefaultAlerts(CryptoCurrency crypto) {
        if (crypto.getPriceChange24h() == null) {
            return;
        }

        double change24h = crypto.getPriceChange24h().doubleValue();

        // Alerta de COMPRA - queda significativa
        if (change24h <= buyThreshold) {
            NotificationMessage message = NotificationMessage.builder()
                    .coinSymbol(crypto.getSymbol().toUpperCase())
                    .coinName(crypto.getName())
                    .currentPrice("$" + decimalFormat.format(crypto.getCurrentPrice()))
                    .changePercentage(decimalFormat.format(change24h) + "%")
                    .alertType(AlertRule.AlertType.PRICE_DECREASE)
                    .message(String.format(
                            "🟢 OPORTUNIDADE DE COMPRA!\n" +
                                    "%s (%s) caiu %.2f%% nas últimas 24h\n" +
                                    "Preço atual: $%s\n" +
                                    "Pode ser um bom momento para comprar!",
                            crypto.getName(),
                            crypto.getSymbol().toUpperCase(),
                            change24h,
                            decimalFormat.format(crypto.getCurrentPrice())
                    ))
                    .build();

            notificationService.sendNotification(message);
        }

        // Alerta de VENDA - alta significativa
        if (change24h >= sellThreshold) {
            NotificationMessage message = NotificationMessage.builder()
                    .coinSymbol(crypto.getSymbol().toUpperCase())
                    .coinName(crypto.getName())
                    .currentPrice("$" + decimalFormat.format(crypto.getCurrentPrice()))
                    .changePercentage(decimalFormat.format(change24h) + "%")
                    .alertType(AlertRule.AlertType.PRICE_INCREASE)
                    .message(String.format(
                            "🔴 CONSIDERE VENDER!\n" +
                                    "%s (%s) subiu %.2f%% nas últimas 24h\n" +
                                    "Preço atual: $%s\n" +
                                    "Pode ser um bom momento para realizar lucros!",
                            crypto.getName(),
                            crypto.getSymbol().toUpperCase(),
                            change24h,
                            decimalFormat.format(crypto.getCurrentPrice())
                    ))
                    .build();

            notificationService.sendNotification(message);
        }

        // Alerta de volatilidade extrema (1h)
        if (crypto.getPriceChange1h() != null) {
            double change1h = crypto.getPriceChange1h().doubleValue();
            if (Math.abs(change1h) >= 15.0) { // Mudança > 15% em 1 hora
                NotificationMessage message = NotificationMessage.builder()
                        .coinSymbol(crypto.getSymbol().toUpperCase())
                        .coinName(crypto.getName())
                        .currentPrice("$" + decimalFormat.format(crypto.getCurrentPrice()))
                        .changePercentage(decimalFormat.format(change1h) + "%")
                        .alertType(change1h > 0 ? AlertRule.AlertType.PRICE_INCREASE : AlertRule.AlertType.PRICE_DECREASE)
                        .message(String.format(
                                "⚠️ VOLATILIDADE EXTREMA!\n" +
                                        "%s (%s) teve variação de %.2f%% na última hora\n" +
                                        "Preço atual: $%s\n" +
                                        "Monitore de perto!",
                                crypto.getName(),
                                crypto.getSymbol().toUpperCase(),
                                change1h,
                                decimalFormat.format(crypto.getCurrentPrice())
                        ))
                        .build();

                notificationService.sendNotification(message);
            }
        }
    }

    /**
     * Verifica alertas personalizados criados pelo usuário
     */
    private void checkCustomAlert(CryptoCurrency crypto, AlertRule rule) {
        try {
            BigDecimal changeValue = getChangeValueByPeriod(crypto, rule.getTimePeriod());

            if (changeValue == null) {
                return;
            }

            boolean shouldAlert = false;
            String alertDirection = "";

            switch (rule.getAlertType()) {
                case PRICE_INCREASE:
                    shouldAlert = changeValue.compareTo(rule.getThresholdValue()) >= 0;
                    alertDirection = "subiu";
                    break;
                case PRICE_DECREASE:
                    shouldAlert = changeValue.compareTo(rule.getThresholdValue().negate()) <= 0;
                    alertDirection = "caiu";
                    break;
                case VOLUME_SPIKE:
                    // Implementar lógica de volume futuramente
                    break;
            }

            if (shouldAlert) {
                NotificationMessage message = NotificationMessage.builder()
                        .coinSymbol(crypto.getSymbol().toUpperCase())
                        .coinName(crypto.getName())
                        .currentPrice("$" + decimalFormat.format(crypto.getCurrentPrice()))
                        .changePercentage(decimalFormat.format(changeValue) + "%")
                        .alertType(rule.getAlertType())
                        .recipient(rule.getNotificationEmail())
                        .message(String.format(
                                "🔔 ALERTA PERSONALIZADO!\n" +
                                        "%s (%s) %s %.2f%% no período de %s\n" +
                                        "Preço atual: $%s\n" +
                                        "Limite configurado: %.2f%%",
                                crypto.getName(),
                                crypto.getSymbol().toUpperCase(),
                                alertDirection,
                                Math.abs(changeValue.doubleValue()),
                                getPeriodDescription(rule.getTimePeriod()),
                                decimalFormat.format(crypto.getCurrentPrice()),
                                rule.getThresholdValue().doubleValue()
                        ))
                        .build();

                notificationService.sendNotification(message);
            }

        } catch (Exception e) {
            log.error("Erro ao verificar alerta personalizado: {}", e.getMessage());
        }
    }

    /**
     * Obtém o valor de mudança baseado no período
     */
    private BigDecimal getChangeValueByPeriod(CryptoCurrency crypto, AlertRule.TimePeriod period) {
        switch (period) {
            case ONE_HOUR:
                return crypto.getPriceChange1h();
            case TWENTY_FOUR_HOURS:
                return crypto.getPriceChange24h();
            case SEVEN_DAYS:
                return crypto.getPriceChange7d();
            default:
                return null;
        }
    }

    /**
     * Retorna descrição do período para mensagens
     */
    private String getPeriodDescription(AlertRule.TimePeriod period) {
        switch (period) {
            case ONE_HOUR:
                return "1 hora";
            case TWENTY_FOUR_HOURS:
                return "24 horas";
            case SEVEN_DAYS:
                return "7 dias";
            default:
                return "período desconhecido";
        }
    }

    /**
     * Cria nova regra de alerta personalizada
     */
    public AlertRule createAlertRule(AlertRule alertRule) {
        return alertRuleRepository.save(alertRule);
    }

    /**
     * Lista todas as regras de alerta ativas
     */
    public List<AlertRule> getActiveAlertRules() {
        return alertRuleRepository.findByIsActiveTrue();
    }

    /**
     * Desativa uma regra de alerta
     */
    public void deactivateAlertRule(Long ruleId) {
        alertRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setIsActive(false);
            alertRuleRepository.save(rule);
        });
    }
}
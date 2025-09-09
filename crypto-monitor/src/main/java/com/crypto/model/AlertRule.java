package com.crypto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_symbol")
    private String coinSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type")
    private AlertType alertType;

    @Column(name = "threshold_value", precision = 10, scale = 2)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_period")
    private TimePeriod timePeriod;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notification_email")
    private String notificationEmail;

    public enum AlertType {
        PRICE_INCREASE, PRICE_DECREASE, VOLUME_SPIKE
    }

    public enum TimePeriod {
        ONE_HOUR, TWENTY_FOUR_HOURS, SEVEN_DAYS
    }
}
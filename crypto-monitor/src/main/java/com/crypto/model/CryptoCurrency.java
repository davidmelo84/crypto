package com.crypto.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.crypto.model.CryptoCurrency;
import lombok.Data;
import java.util.List;
import jakarta.persistence.*;
        import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import lombok.Data;
import lombok.Builder;
import com.crypto.model.AlertRule.AlertType;

// CryptoCurrency.java
package com.crypto.model;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cryptocurrencies")
public class CryptoCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", unique = true)
    @JsonProperty("id")
    private String coinId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("current_price")
    @Column(name = "current_price", precision = 20, scale = 8)
    private BigDecimal currentPrice;

    @JsonProperty("price_change_percentage_1h_in_currency")
    @Column(name = "price_change_1h")
    private BigDecimal priceChange1h;

    @JsonProperty("price_change_percentage_24h_in_currency")
    @Column(name = "price_change_24h")
    private BigDecimal priceChange24h;

    @JsonProperty("price_change_percentage_7d_in_currency")
    @Column(name = "price_change_7d")
    private BigDecimal priceChange7d;

    @JsonProperty("market_cap")
    @Column(name = "market_cap")
    private Long marketCap;

    @JsonProperty("total_volume")
    @Column(name = "total_volume")
    private Long totalVolume;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
}

// CoinGeckoResponse.java
package com.crypto.model.dto;


@Data
public class CoinGeckoResponse {
    private List<CryptoCurrency> data;
}

// AlertRule.java
package com.crypto.model;


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

// NotificationMessage.java
package com.crypto.model.dto;


@Data
@Builder
public class NotificationMessage {
    private String coinSymbol;
    private String coinName;
    private String currentPrice;
    private String changePercentage;
    private AlertType alertType;
    private String message;
    private String recipient;
}
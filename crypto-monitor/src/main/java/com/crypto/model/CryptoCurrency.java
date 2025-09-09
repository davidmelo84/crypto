package com.crypto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
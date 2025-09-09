// NotificationMessage.java
// Localização: src/main/java/com/crypto/model/dto/NotificationMessage.java
package com.crypto.model.dto;

import lombok.Data;
import lombok.Builder;
import com.crypto.model.AlertRule.AlertType;

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
// AlertRuleDTO.java
// Localização: src/main/java/com/crypto/model/dto/AlertRuleDTO.java
package com.crypto.model.dto;

import com.crypto.model.AlertRule;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AlertRuleDTO {

    @NotBlank(message = "Símbolo da moeda é obrigatório")
    @Size(min = 2, max = 10, message = "Símbolo deve ter entre 2 e 10 caracteres")
    private String coinSymbol;

    @NotNull(message = "Tipo de alerta é obrigatório")
    private AlertRule.AlertType alertType;

    @NotNull(message = "Valor do limite é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que 0.01")
    @DecimalMax(value = "1000.0", message = "Valor deve ser menor que 1000")
    private BigDecimal thresholdValue;

    @NotNull(message = "Período de tempo é obrigatório")
    private AlertRule.TimePeriod timePeriod;

    @Email(message = "Email deve ter formato válido")
    private String notificationEmail;

    public AlertRule toEntity() {
        AlertRule rule = new AlertRule();
        rule.setCoinSymbol(this.coinSymbol.toUpperCase());
        rule.setAlertType(this.alertType);
        rule.setThresholdValue(this.thresholdValue);
        rule.setTimePeriod(this.timePeriod);
        rule.setNotificationEmail(this.notificationEmail);
        rule.setIsActive(true);
        return rule;
    }
}
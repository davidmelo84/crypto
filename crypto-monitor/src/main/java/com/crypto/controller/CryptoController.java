package com.crypto.controller;

import com.crypto.model.AlertRule;
import com.crypto.model.CryptoCurrency;
import com.crypto.model.dto.AlertRuleDTO;
import com.crypto.service.AlertService;
import com.crypto.service.CryptoService;
import com.crypto.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Para desenvolvimento - configure adequadamente em produção
public class CryptoController {

    private final CryptoService cryptoService;
    private final AlertService alertService;
    private final NotificationService notificationService;

    /**
     * Busca cotações atuais de todas as criptomoedas monitoradas
     */
    @GetMapping("/current")
    public ResponseEntity<List<CryptoCurrency>> getCurrentPrices() {
        try {
            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();
            return ResponseEntity.ok(cryptos);
        } catch (Exception e) {
            log.error("Erro ao buscar cotações atuais: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busca cotação de uma criptomoeda específica
     */
    @GetMapping("/current/{coinId}")
    public ResponseEntity<CryptoCurrency> getCryptoByCoinId(@PathVariable String coinId) {
        try {
            Optional<CryptoCurrency> crypto = cryptoService.getCryptoByCoinId(coinId);
            return crypto.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar cotação de {}: {}", coinId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busca todas as criptomoedas salvas no banco
     */
    @GetMapping("/saved")
    public ResponseEntity<List<CryptoCurrency>> getAllSavedCryptos() {
        try {
            List<CryptoCurrency> cryptos = cryptoService.getAllSavedCryptos();
            return ResponseEntity.ok(cryptos);
        } catch (Exception e) {
            log.error("Erro ao buscar criptomoedas salvas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busca criptomoeda salva por coinId
     */
    @GetMapping("/saved/{coinId}")
    public ResponseEntity<CryptoCurrency> getSavedCryptoByCoinId(@PathVariable String coinId) {
        try {
            Optional<CryptoCurrency> crypto = cryptoService.getSavedCryptoByCoinId(coinId);
            return crypto.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar crypto salva {}: {}", coinId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Força atualização manual dos preços
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> forceUpdate() {
        try {
            cryptoService.updateCryptoPricesScheduled();
            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Atualização iniciada com sucesso",
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao forçar atualização: {}", e.getMessage());
            Map<String, Object> response = Map.of(
                    "status", "error",
                    "message", "Erro ao iniciar atualização: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cria nova regra de alerta. Usa AlertRuleDTO para validação.
     */
    @PostMapping("/alerts")
    public ResponseEntity<AlertRule> createAlertRule(@Valid @RequestBody AlertRuleDTO alertRuleDTO) {
        try {
            AlertRule alertRule = alertRuleDTO.toEntity();
            AlertRule savedRule = alertService.createAlertRule(alertRule);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRule);
        } catch (Exception e) {
            log.error("Erro ao criar regra de alerta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista todas as regras de alerta ativas
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertRule>> getActiveAlertRules() {
        try {
            List<AlertRule> rules = alertService.getActiveAlertRules();
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            log.error("Erro ao buscar regras de alerta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Desativa uma regra de alerta
     */
    @DeleteMapping("/alerts/{ruleId}")
    public ResponseEntity<Map<String, String>> deactivateAlertRule(@PathVariable Long ruleId) {
        try {
            alertService.deactivateAlertRule(ruleId);
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Regra de alerta desativada com sucesso"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao desativar regra de alerta: {}", e.getMessage());
            Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Erro ao desativar regra de alerta: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Envia notificação de teste
     */
    @PostMapping("/test-notification")
    public ResponseEntity<Map<String, String>> sendTestNotification() {
        try {
            notificationService.sendTestNotification();
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Notificação de teste enviada com sucesso"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de teste: {}", e.getMessage());
            Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Erro ao enviar notificação de teste: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint de status da aplicação
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();
            List<AlertRule> activeRules = alertService.getActiveAlertRules();
            Map<String, Object> status = Map.of(
                    "status", "online",
                    "timestamp", System.currentTimeMillis(),
                    "cryptos_monitored", savedCryptos.size(),
                    "active_alert_rules", activeRules.size(),
                    "last_update", savedCryptos.isEmpty() ? null :
                            savedCryptos.get(0).getLastUpdated()
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Erro ao obter status: {}", e.getMessage());
            Map<String, Object> status = Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }
}
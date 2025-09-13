package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço coordenador que gerencia a comunicação entre CryptoService e AlertService
 * Resolve a dependência circular entre os serviços
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final AlertService alertService;

    /**
     * Atualização automática coordenada a cada 5 minutos
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // 1. Buscar preços atuais
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // 2. Salvar os dados atualizados
            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // 3. Processar alertas com os dados atualizados
            alertService.processAlerts(currentCryptos);

            log.info("✅ Ciclo de monitoramento concluído com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento: {}", e.getMessage(), e);
        }
    }

    /**
     * Processo manual de atualização e verificação de alertas
     */
    public void forceUpdateAndProcessAlerts() {
        try {
            log.info("🚀 Forçando atualização manual...");

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);
                alertService.checkAlertsForCrypto(savedCrypto);
            }

            log.info("✅ Atualização manual concluída. {} moedas processadas", currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    /**
     * Processa alertas para uma criptomoeda específica
     */
    public void processAlertsForCrypto(String coinId) {
        try {
            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);
                                alertService.checkAlertsForCrypto(savedCrypto);
                                log.info("✅ Alertas processados para {}", coinId);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}", coinId, e.getMessage());
        }
    }
}
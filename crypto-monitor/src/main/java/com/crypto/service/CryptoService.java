package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final WebClient webClient;
    private final CryptoCurrencyRepository cryptoRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}")
    private String coinGeckoApiUrl;

    @Value("${crypto.coins:bitcoin,ethereum,cardano,polkadot,chainlink}")
    private String coinsToMonitor;

    // Thresholds automáticos do application.yml
    @Value("${alert.buy.threshold:-5.0}")
    private double buyThreshold;

    @Value("${alert.sell.threshold:10.0}")
    private double sellThreshold;

    @Value("${notification.email.to:testeprojeto0001@gmail.com}")
    private String notificationEmail;

    /**
     * Busca preços atuais da API com retry
     */
    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public List<CryptoCurrency> getCurrentPrices() {
        try {
            String url = String.format("%s/coins/markets?vs_currency=usd&ids=%s" +
                            "&order=market_cap_desc&per_page=100&page=1&sparkline=false" +
                            "&price_change_percentage=1h,24h,7d",
                    coinGeckoApiUrl, coinsToMonitor);

            log.info("Buscando cotações em: {}", url);

            List<CryptoCurrency> cryptos = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CryptoCurrency>>() {})
                    .block();

            if (cryptos != null && !cryptos.isEmpty()) {
                log.info("Encontradas {} criptomoedas", cryptos.size());

                // NOVO: Verificar alertas automáticos
                checkAutomaticAlerts(cryptos);

                return cryptos;
            }

            log.warn("Nenhuma criptomoeda encontrada");
            return List.of();

        } catch (WebClientResponseException e) {
            log.error("Erro ao buscar cotações da CoinGecko API: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao conectar com CoinGecko API", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar cotações: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao buscar cotações", e);
        }
    }

    /**
     * NOVO: Verifica alertas automáticos baseados nos thresholds do application.yml
     */
    private void checkAutomaticAlerts(List<CryptoCurrency> cryptos) {
        log.info("Verificando alertas automáticos - Buy: {}%, Sell: {}%", buyThreshold, sellThreshold);

        for (CryptoCurrency crypto : cryptos) {
            try {
                if (crypto.getPriceChange24h() != null) {
                    double change24h = crypto.getPriceChange24h();

                    log.debug("{} variação 24h: {}%", crypto.getSymbol(), change24h);

                    // Alerta de COMPRA (queda maior ou igual ao threshold)
                    if (change24h <= buyThreshold) {
                        sendBuyAlert(crypto);
                    }

                    // Alerta de VENDA (alta maior ou igual ao threshold)
                    if (change24h >= sellThreshold) {
                        sendSellAlert(crypto);
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao verificar alertas para {}: {}", crypto.getSymbol(), e.getMessage());
            }
        }
    }

    /**
     * NOVO: Envia alerta de oportunidade de compra
     */
    private void sendBuyAlert(CryptoCurrency crypto) {
        String subject = String.format("🟢 OPORTUNIDADE DE COMPRA - %s", crypto.getName());
        String message = String.format(
                "Alerta de Compra - Crypto Monitor\n\n" +
                        "🟢 OPORTUNIDADE DE COMPRA DETECTADA!\n\n" +
                        "Criptomoeda: %s (%s)\n" +
                        "Preço Atual: $%.2f\n" +
                        "Variação 24h: %.2f%%\n" +
                        "Threshold Configurado: %.1f%%\n\n" +
                        "Esta criptomoeda caiu além do seu limite configurado.\n" +
                        "Considere esta oportunidade de compra!\n\n" +
                        "---\n" +
                        "Crypto Monitor - Sistema Automático de Alertas",
                crypto.getName(),
                crypto.getSymbol().toUpperCase(),
                crypto.getCurrentPrice(),
                crypto.getPriceChange24h(),
                buyThreshold
        );

        log.info("🟢 ALERTA DE COMPRA disparado: {} caiu {}% (limite: {}%)",
                crypto.getName(), crypto.getPriceChange24h(), buyThreshold);

        sendEmailNotification(subject, message);
    }

    /**
     * NOVO: Envia alerta de venda
     */
    private void sendSellAlert(CryptoCurrency crypto) {
        String subject = String.format("🔴 ALERTA DE VENDA - %s", crypto.getName());
        String message = String.format(
                "Alerta de Venda - Crypto Monitor\n\n" +
                        "🔴 ALERTA DE VENDA DISPARADO!\n\n" +
                        "Criptomoeda: %s (%s)\n" +
                        "Preço Atual: $%.2f\n" +
                        "Variação 24h: +%.2f%%\n" +
                        "Threshold Configurado: +%.1f%%\n\n" +
                        "Esta criptomoeda subiu além do seu limite configurado.\n" +
                        "Considere realizar lucros!\n\n" +
                        "---\n" +
                        "Crypto Monitor - Sistema Automático de Alertas",
                crypto.getName(),
                crypto.getSymbol().toUpperCase(),
                crypto.getCurrentPrice(),
                crypto.getPriceChange24h(),
                sellThreshold
        );

        log.info("🔴 ALERTA DE VENDA disparado: {} subiu +{}% (limite: +{}%)",
                crypto.getName(), crypto.getPriceChange24h(), sellThreshold);

        sendEmailNotification(subject, message);
    }

    /**
     * NOVO: Envia notificação por email
     */
    private void sendEmailNotification(String subject, String message) {
        try {
            notificationService.sendEmailAlert(notificationEmail, subject, message);
            log.info("Email de alerta enviado para: {}", notificationEmail);
        } catch (Exception e) {
            log.error("Erro ao enviar email de alerta: {}", e.getMessage());
        }
    }

    /**
     * Busca uma criptomoeda específica por coinId com retry
     */
    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            String url = String.format("%s/coins/markets?vs_currency=usd&ids=%s" +
                    "&price_change_percentage=1h,24h,7d", coinGeckoApiUrl, coinId);

            List<CryptoCurrency> cryptos = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CryptoCurrency>>() {})
                    .block();

            return cryptos != null && !cryptos.isEmpty()
                    ? Optional.of(cryptos.get(0))
                    : Optional.empty();

        } catch (Exception e) {
            log.error("Erro ao buscar cotação de {}: {}", coinId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Salva ou atualiza uma criptomoeda no banco de dados
     */
    public CryptoCurrency saveCrypto(CryptoCurrency crypto) {
        return cryptoRepository.findByCoinId(crypto.getCoinId())
                .map(existing -> {
                    existing.setCurrentPrice(crypto.getCurrentPrice());
                    existing.setPriceChange1h(crypto.getPriceChange1h());
                    existing.setPriceChange24h(crypto.getPriceChange24h());
                    existing.setPriceChange7d(crypto.getPriceChange7d());
                    existing.setMarketCap(crypto.getMarketCap());
                    existing.setTotalVolume(crypto.getTotalVolume());
                    return cryptoRepository.save(existing);
                })
                .orElseGet(() -> cryptoRepository.save(crypto));
    }

    /**
     * Retorna todas as criptomoedas salvas no banco
     */
    public List<CryptoCurrency> getAllSavedCryptos() {
        return cryptoRepository.findAllByOrderByMarketCapDesc();
    }

    /**
     * Busca uma criptomoeda salva específica
     */
    public Optional<CryptoCurrency> getSavedCryptoByCoinId(String coinId) {
        return cryptoRepository.findByCoinId(coinId);
    }
}
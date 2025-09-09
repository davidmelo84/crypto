package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final WebClient webClient;
    private final CryptoCurrencyRepository cryptoRepository;
    private final AlertService alertService;

    @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}")
    private String coinGeckoApiUrl;

    @Value("${crypto.coins:bitcoin,ethereum,cardano,polkadot,chainlink}")
    private String coinsToMonitor;

    /**
     * Busca cotações atuais das criptomoedas
     */
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
     * Busca cotação de uma criptomoeda específica
     */
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
     * Salva ou atualiza dados da criptomoeda no banco
     */
    public CryptoCurrency saveCrypto(CryptoCurrency crypto) {
        try {
            Optional<CryptoCurrency> existing = cryptoRepository.findByCoinId(crypto.getCoinId());

            if (existing.isPresent()) {
                CryptoCurrency existingCrypto = existing.get();
                existingCrypto.setCurrentPrice(crypto.getCurrentPrice());
                existingCrypto.setPriceChange1h(crypto.getPriceChange1h());
                existingCrypto.setPriceChange24h(crypto.getPriceChange24h());
                existingCrypto.setPriceChange7d(crypto.getPriceChange7d());
                existingCrypto.setMarketCap(crypto.getMarketCap());
                existingCrypto.setTotalVolume(crypto.getTotalVolume());
                return cryptoRepository.save(existingCrypto);
            } else {
                return cryptoRepository.save(crypto);
            }
        } catch (Exception e) {
            log.error("Erro ao salvar crypto {}: {}", crypto.getSymbol(), e.getMessage());
            throw new RuntimeException("Erro ao salvar dados da criptomoeda", e);
        }
    }

    /**
     * Atualização automática a cada 5 minutos
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    @Async
    public CompletableFuture<Void> updateCryptoPricesScheduled() {
        try {
            log.info("Iniciando atualização automática de preços...");

            List<CryptoCurrency> cryptos = getCurrentPrices();

            for (CryptoCurrency crypto : cryptos) {
                CryptoCurrency savedCrypto = saveCrypto(crypto);

                // Verifica alertas para esta criptomoeda
                alertService.checkAlertsForCrypto(savedCrypto);
            }

            log.info("Atualização automática concluída. {} moedas processadas", cryptos.size());

        } catch (Exception e) {
            log.error("Erro na atualização automática: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Busca todas as criptomoedas salvas no banco
     */
    public List<CryptoCurrency> getAllSavedCryptos() {
        return cryptoRepository.findAllByOrderByMarketCapDesc();
    }

    /**
     * Busca histórico de uma criptomoeda específica
     */
    public Optional<CryptoCurrency> getSavedCryptoByCoinId(String coinId) {
        return cryptoRepository.findByCoinId(coinId);
    }
}
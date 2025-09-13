package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}")
    private String coinGeckoApiUrl;

    @Value("${crypto.coins:bitcoin,ethereum,cardano,polkadot,chainlink}")
    private String coinsToMonitor;

    /**
     * Busca preços atuais da API
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
     * Busca uma criptomoeda específica por coinId
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
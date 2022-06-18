package com.pblinov.binance.futures.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pblinov.binance.futures.api.dto.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
class BinanceRest {
    public static final String BASE_PATH = "/fapi/v1";
    public static final String API_KEY_HEADER = "X-MBX-APIKEY";
    private final BinanceConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    BinanceRest(BinanceConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @SneakyThrows
    ListenKeyResponse getListenKey() {
        var response = httpClient.POST(createUri("/listenKey"))
                .headers(this::authHeaders)
                .send();
        if (response.getStatus() != 200) {
            throw new IllegalStateException("Cannot retrieve listenKey");
        }

        return mapper.readValue(response.getContent(), ListenKeyResponse.class);
    }

    @SneakyThrows
    void extendListenKey(String listenKey) {
        var response = httpClient.newRequest(createUri("/listenKey"))
                .method(HttpMethod.PUT)
                .headers(this::authHeaders)
                .send();
        if (response.getStatus() != 200) {
            log.warn("[{}] Cannot extend listenKey={}: {}", config.getExchangeName(), listenKey, response.getContentAsString());
            throw new IllegalStateException("Cannot retrieve listenKey");
        }
    }

    @SneakyThrows
    boolean ping() {
        log.debug("[{}] Ping", config.getExchangeName());
        return get("/ping").getStatus() == 200;
    }

    @SneakyThrows
    long timestamp() {
        log.debug("[{}] Timestamp", config.getExchangeName());
        var response = get("/time");
        if (response.getStatus() == 200) {
            return mapper.readValue(response.getContent(), TimestampResponse.class).getServerTime();
        } else {
            throw new IllegalStateException("Cannot retrieve server timestamp");
        }
    }

    @SneakyThrows
    void placeOrder(String symbol, String clientOrderId, OrderType type, Side side, double qty, double price, TimeInForce tif) {
        log.info("[{}] Place order with ID: {}", config.getExchangeName(), clientOrderId);

        var response = sendWithTimestamp(httpClient.newRequest(createUri("/order"))
                .method(HttpMethod.POST)
                .param("symbol", symbol)
                .param("newClientOrderId", clientOrderId)
                .param("side", Objects.toString(side))
                .param("type", Objects.toString(type))
                .param("quantity", Objects.toString(qty))
                .param("price", Objects.toString(price))
                .param("timeInForce", Objects.toString(tif)));

        if (response.getStatus() != 200) {
            log.warn("[{}] Place order response: {}", config.getExchangeName(), response.getContentAsString());
            throw new IllegalStateException("Cannot place order");
        } else {
            log.debug("[{}] Place order response: {}", config.getExchangeName(), response.getContentAsString());
        }
    }

    @SneakyThrows
    Order queryOrder(String symbol, String clientOrderId) {
        log.info("[{}] Query order with ID: {}", config.getExchangeName(), clientOrderId);

        var response = sendWithTimestamp(httpClient.newRequest(createUri("/order"))
                .method(HttpMethod.GET)
                .param("symbol", symbol)
                .param("origClientOrderId", clientOrderId));

        switch (response.getStatus()) {
            case 200:
                log.debug("[{}] Query order response: {}", config.getExchangeName(), response.getContentAsString());
                // {"orderId":3046231366,"symbol":"BTCUSDT","status":"NEW","clientOrderId":"Lt2LC3grCRzxJfc6MZf1IF","price":"28700","avgPrice":"0.00000","origQty":"0.010","executedQty":"0","cumQuote":"0","timeInForce":"GTC","type":"LIMIT","reduceOnly":false,"closePosition":false,"side":"SELL","positionSide":"BOTH","stopPrice":"0","workingType":"CONTRACT_PRICE","priceProtect":false,"origType":"LIMIT","time":1654987000652,"updateTime":1654987000652}
                return mapper.readValue(response.getContent(), Order.class);
            case 400:
                try {
                    var error = mapper.readValue(response.getContent(), ErrorResponse.class);
                    throw ProcessingException.of(error.getCode(), error.getMsg());
                } catch (IOException e) {
                    log.error("[{}] Cannot parse order query response: {}", config.getExchangeName(), response.getContentAsString());
                    throw new RuntimeException(e);
                }
            default:
                log.error("[{}] Query order response: {}", config.getExchangeName(), response.getContentAsString());
                throw new IllegalStateException("Cannot query order");
        }
    }

    @SneakyThrows
    Order cancelOrder(String symbol, String clientOrderId) {
        log.info("[{}] Cancel order with ID: {}", config.getExchangeName(), clientOrderId);
        var response = sendWithTimestamp(httpClient.newRequest(createUri("/order"))
                .method(HttpMethod.DELETE)
                .param("symbol", symbol)
                .param("origClientOrderId", clientOrderId));

        switch (response.getStatus()) {
            case 200:
                log.debug("[{}] Cancel order response: {}", config.getExchangeName(), response.getContentAsString());
                return mapper.readValue(response.getContent(), Order.class);
            case 400:
                try {
                    var error = mapper.readValue(response.getContent(), ErrorResponse.class);
                    throw ProcessingException.of(error.getCode(), error.getMsg());
                } catch (IOException e) {
                    log.error("[{}] Cannot parse cancel order response: {}", config.getExchangeName(), response.getContentAsString());
                    throw new RuntimeException(e);
                }
            default:
                log.error("[{}] Cancel order response: {}", config.getExchangeName(), response.getContentAsString());
                throw new IllegalStateException("Cannot cancel order");
        }
    }

    private ContentResponse sendWithTimestamp(Request request) throws InterruptedException, TimeoutException, ExecutionException, NoSuchAlgorithmException, InvalidKeyException {
        return sendWithSignature(withTimestamp(request));
    }

    private Request withTimestamp(Request request) {
        return request
                .param("recvWindow", Objects.toString(30_000))
                .param("timestamp", Objects.toString(System.currentTimeMillis() - 15_000)); //TODO: Add timestamp correction to settings
    }

    private ContentResponse sendWithSignature(Request request) throws InterruptedException, TimeoutException, ExecutionException, NoSuchAlgorithmException, InvalidKeyException {
        return request
                .param("signature", SignatureUtils.sign(request.getQuery(), config.getApiSecret()))
                .headers(this::authHeaders)
                .send();
    }

    private void authHeaders(HttpFields.Mutable headers) {
        headers.put(API_KEY_HEADER, config.getApiKey());
    }

    private ContentResponse get(String path) throws ExecutionException, InterruptedException, TimeoutException {
        return httpClient.GET(createUri(path));
    }

    private URI createUri(String path) {
        return URI.create(config.getHttpUrl() + BASE_PATH + path);
    }
}

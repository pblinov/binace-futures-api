package com.pblinov.binance.futures.api;

import com.pblinov.binance.futures.api.dto.Order;
import com.pblinov.binance.futures.api.dto.OrderType;
import com.pblinov.binance.futures.api.dto.Side;
import com.pblinov.binance.futures.api.dto.TimeInForce;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BinanceExchange implements Exchange {
    public static final RetryConfig RETRY_CONFIG = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryExceptions(IOException.class, TimeoutException.class, ProcessingException.class)
            .ignoreExceptions(UnrecoverableProcessingException.class)
            .failAfterMaxAttempts(true)
            .build();
    private final BinanceRest rest;
    private final BinanceWebSocket webSocket;

    public BinanceExchange(BinanceConfig config, EventListener eventListener) {
        var httpClient = new HttpClient();
        this.rest = new BinanceRest(config, httpClient);
        this.webSocket = new BinanceWebSocket(httpClient, config, rest, eventListener);
    }

    public void start() throws Exception {
        webSocket.start();
    }

    public void stop() throws Exception {
        webSocket.stop();
    }

    @Override
    public boolean ping() {
        return rest.ping();
    }

    @Override
    public long timestamp() {
        return rest.timestamp();
    }

    /**
     * POST /fapi/v1/order (HMAC SHA256)
     */
    @Override
    public void placeOrder(String symbol, String clientOrderId, OrderType type, Side side, double qty, double price, TimeInForce tif) {
        rest.placeOrder(symbol, clientOrderId, type, side, qty, price, tif);
    }

    /**
     * GET /fapi/v1/order (HMAC SHA256)
     */
    @Override
    public Order queryOrder(String symbol, String clientOrderId) {
        return Retry.of("query", RETRY_CONFIG)
                .executeSupplier(() -> rest.queryOrder(symbol, clientOrderId));
    }

    /**
     * DELETE /fapi/v1/order (HMAC SHA256)
     */
    @Override
    public Order cancelOrder(String symbol, String clientOrderId) {
        return Retry.of("cancel", RETRY_CONFIG)
                .executeSupplier(() -> {
                    try {
                        return rest.cancelOrder(symbol, clientOrderId);
                    } catch (Exception e) {
                        var order = queryOrder(symbol, clientOrderId);
                        if (order.getStatus().isFinal()) {
                            log.debug("Order already has a final state");
                            return order;
                        }
                        throw e;
                    }
                });
    }

    public void connect() {
        webSocket.connect();
    }
}

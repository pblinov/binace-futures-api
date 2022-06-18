package com.pblinov.binance.futures.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pblinov.binance.futures.api.dto.Event;
import com.pblinov.binance.futures.api.dto.OrderUpdateEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WebSocketPingPongListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.codec.binary.Hex.encodeHex;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

@Slf4j
class BinanceWebSocket implements WebSocketListener, WebSocketPingPongListener {
    private final BinanceConfig config;
    private final BinanceRest rest;
    private final WebSocketClient webSocketClient;
    private final EventListener eventListener;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService extendListenKeyExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
    private final SocketState state = new SocketState();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    BinanceWebSocket(HttpClient httpClient, BinanceConfig config, BinanceRest rest, EventListener eventListener) {
        this.config = config;
        this.rest = rest;
        this.eventListener = eventListener;
        webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setMaxTextMessageSize(8 * 1024); // TODO: Move to settings
    }

    public void start() throws Exception {
        webSocketClient.start();
    }

    public void stop() throws Exception {
        disconnect();
        webSocketClient.stop();
    }

    @SneakyThrows
    public void connect() {
        connected.set(true);
        reconnect();

        // Schedule WS session refresh
        extendListenKeyExecutor.scheduleAtFixedRate(this::extendListenKey, 50, 50, TimeUnit.MINUTES);
        keepAliveExecutor.scheduleAtFixedRate(this::keepAlive, 5, 5, TimeUnit.MINUTES);
    }

    private void extendListenKey() {
        log.debug("[{}] Extend key", config.getExchangeName());
        var listenKey = state.getListenKey();
        try {
            rest.extendListenKey(listenKey);
        } catch (Exception e) {
            //TODO: Process exception (retry, ...)
            log.error("[{}] Cannot extend key: {}", config.getExchangeName(), listenKey);
        }
    }

    private void keepAlive() {
        log.debug("[{}] Keep alive", config.getExchangeName());
        //TODO: Implement (send ping, check SocketState.timestamp, ...)
    }

    @SneakyThrows
    private void reconnect() {
        if (connected.get()) {
            var key = rest.getListenKey();
            var serverURI = URI.create(config.getWsUrl() + "/ws/" + key.getListenKey());
            log.info("[{}] Connecting to {}", config.getExchangeName(), serverURI);

            // Start session
            var session = webSocketClient.connect(this, serverURI);

            state.update(key.getListenKey(), session);
        }
    }

    public void disconnect() {
        connected.set(false);
        extendListenKeyExecutor.shutdown();
        keepAliveExecutor.shutdown();
    }

    @Override
    public void onWebSocketConnect(Session session) {
        log.info("[{}] WS connect", config.getExchangeName());
        state.ping();
        session.setIdleTimeout(Duration.ofMinutes(10)); // TODO: Move to settings
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.info("[{}] WS close", config.getExchangeName());
        //TODO: Reconnect only when WS is not closed explicitly (see disconnect)
        reconnect();
        //TODO: Reconcile order states if required
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        log.error("[{}] WS error", config.getExchangeName(), cause);
    }

    @Override
    public void onWebSocketText(String message) {
        log.debug("[{}] WS message: {}", config.getExchangeName(), message);
        state.ping();
        try {
            var event = mapper.readValue(message, Event.class);
            if (event instanceof OrderUpdateEvent) {
                eventListener.onOrderUpdate((OrderUpdateEvent) event);
            }
        } catch (JsonProcessingException e) {
            log.error("[{}] Cannot parse message: {}", config.getExchangeName(), message, e);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        log.warn("[{}] Unexpected binary message received from WS: {}", config.getExchangeName(), new String(encodeHex(payload, offset, len, false)));
        state.ping();
    }

    @Override
    public void onWebSocketPing(ByteBuffer payload) {
        log.debug("Ping: {}", encodeHexString(payload, false));
        state.ping();
    }

    @Override
    public void onWebSocketPong(ByteBuffer payload) {
        log.debug("Pong: {}", encodeHex(payload));
        state.ping();
    }
}

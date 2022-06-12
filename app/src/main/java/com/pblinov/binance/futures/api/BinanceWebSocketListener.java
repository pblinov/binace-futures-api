package com.pblinov.binance.futures.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pblinov.binance.futures.api.dto.Event;
import com.pblinov.binance.futures.api.dto.OrderUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.time.Duration;

@Slf4j
class BinanceWebSocketListener implements WebSocketListener {
    private final ObjectMapper mapper = new ObjectMapper();
    private final EventListener eventListener;
    private final Exchange exchange;
    private final String exchangeName;

    public BinanceWebSocketListener(String exchangeName, Exchange exchange, EventListener eventListener) {
        this.exchangeName = exchangeName;
        this.exchange = exchange;
        this.eventListener = eventListener;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        log.info("[{}] WS connect", exchangeName);
        session.setIdleTimeout(Duration.ofMinutes(10)); // TODO: Move to settings
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.info("[{}] WS close", exchangeName);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        log.error("[{}] WS error", exchangeName, cause);
    }

    @Override
    public void onWebSocketText(String message) {
        log.debug("[{}] WS message: {}", exchangeName, message);
        try {
            var event = mapper.readValue(message, Event.class);
            if (event instanceof OrderUpdateEvent) {
                eventListener.onOrderUpdate(exchange, (OrderUpdateEvent) event);
            }
        } catch (JsonProcessingException e) {
            log.error("[{}] Cannot parse message: {}", exchangeName, message, e);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        log.warn("[{}] Unexpected binary message received from WS", exchangeName); //TODO: Log received message in hex format
    }
}

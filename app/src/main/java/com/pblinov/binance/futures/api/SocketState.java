package com.pblinov.binance.futures.api;

import org.eclipse.jetty.websocket.api.Session;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

class SocketState {
    private String listenKey;
    private CompletableFuture<Session> session;
    private Instant timestamp;

    synchronized void update(String listenKey, CompletableFuture<Session> session) {
        this.listenKey = listenKey;
        this.session = session;
        ping();
    }

    synchronized void ping() {
        this.timestamp = Instant.now();
    }

    synchronized String getListenKey() {
        return listenKey;
    }

    synchronized CompletableFuture<Session> getSession() {
        return session;
    }

    synchronized Instant getTimestamp() {
        return timestamp;
    }
}

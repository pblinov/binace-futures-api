package com.pblinov.binance.futures.api;

import com.pblinov.binance.futures.api.dto.OrderType;
import com.pblinov.binance.futures.api.dto.Side;
import com.pblinov.binance.futures.api.dto.TimeInForce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Exchange {
    boolean ping() throws ExecutionException, InterruptedException, TimeoutException;

    long timestamp() throws ExecutionException, InterruptedException, TimeoutException, IOException;

    void placeOrder(String symbol, OrderType type, Side side, double qty, double price, TimeInForce tif) throws ExecutionException, InterruptedException, TimeoutException, NoSuchAlgorithmException, InvalidKeyException;

    void queryOrder(String symbol, String clientOrderId) throws NoSuchAlgorithmException, ExecutionException, InvalidKeyException, InterruptedException, TimeoutException;

    void cancelOrder();
}

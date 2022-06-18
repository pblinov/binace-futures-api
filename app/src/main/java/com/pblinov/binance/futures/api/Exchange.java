package com.pblinov.binance.futures.api;

import com.pblinov.binance.futures.api.dto.Order;
import com.pblinov.binance.futures.api.dto.OrderType;
import com.pblinov.binance.futures.api.dto.Side;
import com.pblinov.binance.futures.api.dto.TimeInForce;

public interface Exchange {
    boolean ping();

    long timestamp();

    void placeOrder(String symbol, String clientOrderId, OrderType type, Side side, double qty, double price, TimeInForce tif);

    Order queryOrder(String symbol, String clientOrderId);

    Order cancelOrder(String symbol, String clientOrderId);
}

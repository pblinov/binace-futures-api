package com.pblinov.binance.futures.api;

import com.pblinov.binance.futures.api.dto.OrderUpdateEvent;

public interface EventListener {
    void onOrderUpdate(OrderUpdateEvent orderUpdate);
}

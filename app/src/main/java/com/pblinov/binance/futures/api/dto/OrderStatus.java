package com.pblinov.binance.futures.api.dto;

public enum OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    EXPIRED,
    NEW_INSURANCE,
    NEW_ADL;

    public boolean isFinal() {
        return this == CANCELED || this == FILLED || this == EXPIRED;
    }
}

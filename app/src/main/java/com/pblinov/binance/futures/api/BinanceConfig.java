package com.pblinov.binance.futures.api;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BinanceConfig {
    private final String exchangeName;
    private final String httpUrl;
    private final String wsUrl;
    private final String apiKey;
    private final String apiSecret;
}

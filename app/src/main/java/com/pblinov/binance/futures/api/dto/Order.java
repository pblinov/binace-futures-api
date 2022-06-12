package com.pblinov.binance.futures.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pblinov.binance.futures.api.dto.OrderStatus;
import com.pblinov.binance.futures.api.dto.OrderType;
import com.pblinov.binance.futures.api.dto.Side;
import com.pblinov.binance.futures.api.dto.TimeInForce;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    private long orderId;
    private String symbol;
    private OrderStatus status;
    private String clientOrderId;
    private double price;
    private double origQty;
    private double executedQty;
    private TimeInForce timeInForce;
    private OrderType type;
    private Side side;
}

package com.pblinov.binance.futures.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderUpdateEvent extends Event {
    @JsonProperty("o")
    private OrderTradeUpdate payload;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderTradeUpdate {
        @JsonProperty("s")
        private String symbol;
        @JsonProperty("c")
        private String clientOrderId;
        @JsonProperty("S")
        private Side side;
        @JsonProperty("o")
        private OrderType orderType;
        @JsonProperty("f")
        private TimeInForce timeInForce;
        @JsonProperty("x")
        private ExecutionType executionType;
        @JsonProperty("X")
        private OrderStatus orderStatus;

        /**
         * Original Price.
         * Alternatively we can use BigDecimal.
         */
        @JsonProperty("p")
        private Double price;

        /**
         * Original Quantity.
         * Alternatively we can use BigDecimal.
         */
        @JsonProperty("q")
        private Double qty;
    }
}

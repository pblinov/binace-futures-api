package com.pblinov.binance.futures.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "e")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderUpdateEvent.class, name = "ORDER_TRADE_UPDATE"),
        @JsonSubTypes.Type(value = AccountUpdateEvent.class, name = "ACCOUNT_UPDATE")
        //TODO: Add other types: listenKeyExpired, MARGIN_CALL, ACCOUNT_CONFIG_UPDATE
})
public class Event {
    @JsonProperty("T")
    private long eventTime;
    @JsonProperty("E")
    private Long transactionTime;
}

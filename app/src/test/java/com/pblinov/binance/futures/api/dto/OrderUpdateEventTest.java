package com.pblinov.binance.futures.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;

import java.io.IOException;

import static com.pblinov.binance.futures.api.dto.OrderType.LIMIT;
import static com.pblinov.binance.futures.api.dto.Side.SELL;
import static com.pblinov.binance.futures.api.dto.TimeInForce.GTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class OrderUpdateEventTest extends TestCase {
    private final ObjectMapper mapper = new ObjectMapper();

    public void testParser() throws IOException {
        var result = mapper.readValue(getClass().getResourceAsStream("orderTradeUpdateReal.json"), Event.class);
        assertThat(result, CoreMatchers.instanceOf(OrderUpdateEvent.class));

        var payload = ((OrderUpdateEvent) result).getPayload();
        assertThat(payload.getSymbol(), is("BTCUSDT"));
        assertThat(payload.getClientOrderId(), is("web_3NqnbINGX7F0WcrGns2K"));
        assertThat(payload.getSide(), is(SELL));
        assertThat(payload.getOrderType(), is(LIMIT));
        assertThat(payload.getTimeInForce(), is(GTC));
        assertThat(payload.getPrice(), is(28501.20));
        assertThat(payload.getQty(), is(0.012));
    }
}
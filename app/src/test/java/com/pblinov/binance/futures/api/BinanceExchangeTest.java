package com.pblinov.binance.futures.api;

import com.pblinov.binance.futures.api.dto.ExecutionType;
import com.pblinov.binance.futures.api.dto.OrderUpdateEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.pblinov.binance.futures.api.dto.OrderStatus.CANCELED;
import static com.pblinov.binance.futures.api.dto.OrderStatus.NEW;
import static com.pblinov.binance.futures.api.dto.OrderType.LIMIT;
import static com.pblinov.binance.futures.api.dto.Side.SELL;
import static com.pblinov.binance.futures.api.dto.TimeInForce.GTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * Integration test.
 * TODO: Move out of unit tests
 */
@Slf4j
public class BinanceExchangeTest {
    private BinanceExchange exchange;
    private CountDownLatch latch;

    public static final String SYMBOL = "BTCUSDT";
    public String orderId;

    @Before
    public void setUp() throws Exception {
        latch = new CountDownLatch(1);
        exchange = new BinanceExchange(BinanceConfig.builder()
                .exchangeName("Binance Futures")
                .httpUrl("https://testnet.binancefuture.com")
                .wsUrl("wss://stream.binancefuture.com")
                .apiKey("2001bb6af62d27c7993730801dd9dab763bd6f6c4e1a736331861b7c30b8e950")
                .apiSecret("802990f5ee6ffa2491f8dabf2c3fcf10edae15a71eaaa74b482586d26ef87890")
                .build(),
                this::onOrderUpdate);

        exchange.start();

        if (exchange.ping()) {
            log.info("REST API is available");
            exchange.connect();
            log.info("Time difference: {}ms", System.currentTimeMillis() - ((Exchange) exchange).timestamp());
            orderId = Long.toString(System.currentTimeMillis());
        } else {
            throw new IllegalStateException("Exchange is unavailable");
        }
    }

    @Test
    public void testPlaceAndCancelOrder() throws InterruptedException {
        exchange.placeOrder(SYMBOL, orderId, LIMIT, SELL, 0.01, 28700.0, GTC);
        var order = exchange.queryOrder(SYMBOL, orderId);
        assertThat(order.getStatus(), is(NEW));
        order = exchange.cancelOrder(SYMBOL, orderId);
        assertThat(order.getStatus(), is(CANCELED));
        if (!latch.await(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS)) {
            fail("Orders hasn't cancelled in timeout");
        }
    }

    @Test
    public void testCancelAlreadyCancelledOrder() {
        exchange.placeOrder(SYMBOL, orderId, LIMIT, SELL, 0.01, 28700.0, GTC);
        log.info("Order: {}", exchange.queryOrder(SYMBOL, orderId));
        exchange.cancelOrder(SYMBOL, orderId);

        var order = exchange.cancelOrder(SYMBOL, orderId);
        assertThat(order.getStatus(), is(CANCELED));
    }

    @Test(expected = UnrecoverableProcessingException.class)
    public void testCancelOrderWithInvalidSymbol() {
        exchange.cancelOrder("INVALID", orderId);
    }

    @Test(expected = ProcessingException.class)
    public void testCancelOrderWithInvalidID() {
        try {
            // Only to demonstrate recoverable issues we are considering invalid ID as recoverable issue
            exchange.cancelOrder(SYMBOL, "WrongID");
        } catch (UnrecoverableProcessingException e) {
            fail("Invalid exception");
        }
    }

    @SneakyThrows
    private void onOrderUpdate(OrderUpdateEvent orderUpdate) {
        var payload = orderUpdate.getPayload();
        log.info("Order with ID={} has status {}({})", payload.getClientOrderId(), payload.getOrderStatus(), payload.getExecutionType());
        if (payload.getExecutionType() == ExecutionType.CANCELED) {
            latch.countDown();
        }
    }
}
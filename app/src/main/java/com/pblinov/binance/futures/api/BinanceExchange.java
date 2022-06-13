package com.pblinov.binance.futures.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pblinov.binance.futures.api.dto.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BinanceExchange implements Exchange {
    private static final String BASE_PATH = "/fapi/v1";
    private static final String API_KEY_HEADER = "X-MBX-APIKEY";
    private static final String EXCHANGE_NAME = "Binance Futures";
    private final String httpUrl;
    private final String wsUrl;

    private final String apiKey;
    private final String apiSecret;
    private final HttpClient httpClient;
    private final WebSocketClient webSocketClient;
    private final BinanceWebSocketListener wsListener;
    private final ObjectMapper mapper = new ObjectMapper();

    public BinanceExchange(String httpUrl, String wsUrl, String apiKey, String apiSecret, EventListener eventListener) {
        this.httpUrl = httpUrl;
        this.wsUrl = wsUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.wsListener = new BinanceWebSocketListener(EXCHANGE_NAME, this, eventListener);
        httpClient = new HttpClient();
        webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setMaxTextMessageSize(8 * 1024); // TODO: Move to settings
    }

    public void start() throws Exception {
        webSocketClient.start();
    }

    public void stop() throws Exception {
        webSocketClient.stop();
    }

    @Override
    @SneakyThrows
    public boolean ping() {
        log.debug("[{}] Ping", EXCHANGE_NAME);
        return get("/ping").getStatus() == 200;
    }

    @Override
    @SneakyThrows
    public long timestamp() {
        log.debug("[{}] Timestamp", EXCHANGE_NAME);
        var response = get("/time");
        if (response.getStatus() == 200) {
            return mapper.readValue(response.getContent(), TimestampResponse.class).getServerTime();
        } else {
            throw new IllegalStateException("Cannot retrieve server timestamp");
        }
    }

    /**
     * POST /fapi/v1/order (HMAC SHA256)
     */
    @Override
    @SneakyThrows
    public void placeOrder(String symbol, String clientOrderId, OrderType type, Side side, double qty, double price, TimeInForce tif) {
        log.info("[{}] Place order with ID: {}", EXCHANGE_NAME, clientOrderId);

        var response = sendWithTimestamp(httpClient.newRequest(createUri("/order"))
                .method(HttpMethod.POST)
                .param("symbol", symbol)
                .param("newClientOrderId", clientOrderId)
                .param("side", Objects.toString(side))
                .param("type", Objects.toString(type))
                .param("quantity", Objects.toString(qty))
                .param("price", Objects.toString(price))
                .param("timeInForce", Objects.toString(tif)));

        if (response.getStatus() != 200) {
            log.warn("[{}] Place order response: {}", EXCHANGE_NAME, response.getContentAsString());
            throw new IllegalStateException("Cannot place order");
        } else {
            log.debug("[{}] Place order response: {}", EXCHANGE_NAME, response.getContentAsString());
        }
    }

    /**
     * GET /fapi/v1/order (HMAC SHA256)
     */
    @Override
    @SneakyThrows
    public Order queryOrder(String symbol, String clientOrderId) {
        log.info("[{}] Query order with ID: {}", EXCHANGE_NAME, clientOrderId);

        var response = sendWithTimestamp(httpClient.newRequest(createUri("/order"))
                .method(HttpMethod.GET)
                .param("symbol", symbol)
                .param("origClientOrderId", clientOrderId));

        if (response.getStatus() != 200) {
            log.warn("[{}] Query order response: {}", EXCHANGE_NAME, response.getContentAsString());
            throw new IllegalStateException("Cannot get order details");
        } else {
            log.debug("[{}] Query order response: {}", EXCHANGE_NAME, response.getContentAsString());
            // {"orderId":3046231366,"symbol":"BTCUSDT","status":"NEW","clientOrderId":"Lt2LC3grCRzxJfc6MZf1IF","price":"28700","avgPrice":"0.00000","origQty":"0.010","executedQty":"0","cumQuote":"0","timeInForce":"GTC","type":"LIMIT","reduceOnly":false,"closePosition":false,"side":"SELL","positionSide":"BOTH","stopPrice":"0","workingType":"CONTRACT_PRICE","priceProtect":false,"origType":"LIMIT","time":1654987000652,"updateTime":1654987000652}
            return mapper.readValue(response.getContent(), Order.class);
        }
    }

    /**
     * DELETE /fapi/v1/order (HMAC SHA256)
     */
    @Override
    @SneakyThrows
    public void cancelOrder(String symbol, String clientOrderId) {
        log.info("[{}] Cancel order with ID: {}", EXCHANGE_NAME, clientOrderId);
        var response = sendWithTimestamp(httpClient.newRequest(createUri("/order"))
                .method(HttpMethod.DELETE)
                .param("symbol", symbol)
                .param("origClientOrderId", clientOrderId));

        if (response.getStatus() != 200) {
            log.warn("[{}] Cancel order response: {}", EXCHANGE_NAME, response.getContentAsString());
            throw new IllegalStateException("Cannot cancel order");
        } else {
            log.debug("[{}] Cancel order response: {}", EXCHANGE_NAME, response.getContentAsString());
        }
    }

    private ContentResponse sendWithTimestamp(Request request) throws InterruptedException, TimeoutException, ExecutionException, NoSuchAlgorithmException, InvalidKeyException {
        return sendWithSignature(withTimestamp(request));
    }

    private Request withTimestamp(Request request) {
        return request
                .param("recvWindow", Objects.toString(30_000))
                .param("timestamp", Objects.toString(System.currentTimeMillis() - 15_000)); //TODO: Add timestamp correction to settings
    }

    private ContentResponse sendWithSignature(Request request) throws InterruptedException, TimeoutException, ExecutionException, NoSuchAlgorithmException, InvalidKeyException {
        return request
                .param("signature", SignatureUtils.sign(request.getQuery(), apiSecret))
                .headers(this::authHeaders)
                .send();
    }

    private void authHeaders(HttpFields.Mutable headers) {
        headers.put(API_KEY_HEADER, apiKey);
    }

    private ContentResponse get(String path) throws ExecutionException, InterruptedException, TimeoutException {
        return httpClient.GET(createUri(path));
    }

    private URI createUri(String path) {
        return URI.create(httpUrl + BASE_PATH + path);
    }

    public void connect() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        var key = getListenKey();
        var serverURI = URI.create(wsUrl + "/ws/" + key.getListenKey());
        log.info("[{}] Connecting to {}", EXCHANGE_NAME, serverURI);
        webSocketClient.connect(wsListener, serverURI);
    }

    private ListenKeyResponse getListenKey() throws InterruptedException, TimeoutException, ExecutionException, IOException {
        var response = httpClient.POST(createUri("/listenKey"))
                .headers(this::authHeaders)
                .send();
        if (response.getStatus() != 200) {
            throw new IllegalStateException("Cannot retrieve listenKey");
        }

        return mapper.readValue(response.getContent(), ListenKeyResponse.class);
    }
}

# Binance futures API

## Description
Binance futures test net (https://testnet.binancefuture.com/):
implement on java REST + WS client:

REST requests:
- place order
- request order status
- cancel order

WS:
- Receive updates from WS

## Run application
- Use **com.pblinov.binance.futures.App** to run application
- Main logic in **com.pblinov.binance.futures.api.BinanceExchange**

## Endpoints
* The base API endpoint is: https://fapi.binance.com
* The REST baseurl for testnet is https://testnet.binancefuture.com
* Base Url 1: wss://fstream.binance.com
* Base Url 2: wss://fstream-auth.binance.com
* The Websocket baseurl for testnet is wss://stream.binancefuture.com

## Test API keys
* **API Key:** 2001bb6af62d27c7993730801dd9dab763bd6f6c4e1a736331861b7c30b8e950
* **API Secret:** 802990f5ee6ffa2491f8dabf2c3fcf10edae15a71eaaa74b482586d26ef87890

## Links
* [API Docs](https://binance-docs.github.io/apidocs/futures/en)
* [REST - New Order (TRADE)](https://binance-docs.github.io/apidocs/futures/en/#new-order-trade)
* [WS - User Data Streams](https://binance-docs.github.io/apidocs/futures/en/#user-data-streams)
* [How to Test My Functions on Binance Testnet](https://www.binance.com/en/support/faq/ab78f9a1b8824cf0a106b4229c76496d)
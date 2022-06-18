package com.pblinov.binance.futures.api;

public class UnrecoverableProcessingException extends ProcessingException {
    public UnrecoverableProcessingException(long code, String message) {
        super(code, message);
    }
}

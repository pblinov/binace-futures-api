package com.pblinov.binance.futures.api;

public class ProcessingException extends RuntimeException {
    public static final long BAD_SYMBOL = -1121;

    private final long code;

    public ProcessingException(long code, String message) {
        super(message);
        this.code = code;
    }

    public long getCode() {
        return code;
    }

    public static ProcessingException of(long code, String message) {
        if (isUnrecoverable(code)) {
            return new UnrecoverableProcessingException(code, message);
        } else {
            return new ProcessingException(code, message);
        }
    }

    private static boolean isUnrecoverable(long code) {
        // Only BAD_SYMBOL is used here to demonstrate unrecoverable issue processing
        // TODO: All codes should re-viewed to distinct recoverable & unrecoverable issues
        return code == BAD_SYMBOL;
    }
}

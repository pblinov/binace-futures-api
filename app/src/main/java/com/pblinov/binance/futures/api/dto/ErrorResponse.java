package com.pblinov.binance.futures.api.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private long code;
    private String msg;
}

package com.shopai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class StockException extends RuntimeException {
    public StockException(String message) { super(message); }
    public StockException(String productName, int requested, int available) {
        super(String.format("'%s' için stok yetersiz. İstenen: %d, Mevcut: %d", productName, requested, available));
    }
}

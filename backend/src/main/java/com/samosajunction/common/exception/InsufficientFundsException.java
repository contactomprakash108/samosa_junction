package com.samosajunction.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", message);
    }
}

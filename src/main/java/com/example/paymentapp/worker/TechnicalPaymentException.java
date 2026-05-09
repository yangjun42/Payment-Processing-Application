package com.example.paymentapp.worker;

public class TechnicalPaymentException extends RuntimeException {

    private final Integer httpStatus;
    private final String rawBody;

    public TechnicalPaymentException(String message, Integer httpStatus, String rawBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.rawBody = rawBody;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String rawBody() {
        return rawBody;
    }
}


package com.example.paymentapp.worker;

public record ExternalPaymentResult(
        String externalPaymentId,
        String status,
        String message,
        int httpStatus,
        String rawBody) {

    public boolean approved() {
        return "APPROVED".equalsIgnoreCase(status);
    }
}


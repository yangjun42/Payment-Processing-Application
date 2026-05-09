package com.example.paymentapp.worker;

public record ExternalPaymentResponse(String externalPaymentId, String status, String message) {}


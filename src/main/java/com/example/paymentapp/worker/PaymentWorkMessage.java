package com.example.paymentapp.worker;

import java.util.UUID;

public record PaymentWorkMessage(UUID paymentId) {}


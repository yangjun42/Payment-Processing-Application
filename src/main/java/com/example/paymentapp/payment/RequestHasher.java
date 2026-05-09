package com.example.paymentapp.payment;

import com.example.paymentapp.api.PaymentCreateRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class RequestHasher {

    private RequestHasher() {}

    public static String hash(PaymentCreateRequest request) {
        BigDecimal normalizedAmount = request.amount().stripTrailingZeros();
        String canonical = normalizedAmount.toPlainString()
                + "|" + request.currency().toUpperCase(Locale.ROOT)
                + "|" + request.reference();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK", exception);
        }
    }
}


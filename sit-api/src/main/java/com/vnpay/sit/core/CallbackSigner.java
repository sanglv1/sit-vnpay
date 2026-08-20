package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CallbackSigner {

    private CallbackSigner() {
    }

    public static String sign(Map<String, String> params, String secretKey, PaymentFlow flow) {
        SigningProfile profile = profileFor(flow);
        String hashData = buildHashData(params, profile);
        return VnPayHashUtils.hmacSha512(secretKey, hashData);
    }

    public static void attachHash(Map<String, String> params, String secretKey, PaymentFlow flow) {
        String hash = sign(params, secretKey, flow);
        params.put(hashFieldFor(flow), hash);
    }

    public static String hashFieldFor(PaymentFlow flow) {
        return CallbackFields.secureHashKey(flow);
    }

    private static SigningProfile profileFor(PaymentFlow flow) {
        return switch (flow) {
            case PAY, INSTALMENT, QR_DIRECT -> new SigningProfile(StandardCharsets.UTF_8, true, false, false,
                    "vnp_SecureHash", "vnp_SecureHashType");
            case TOKEN, RECURRING -> new SigningProfile(StandardCharsets.US_ASCII, false, true, false,
                    "vnp_secure_hash", "vnp_secure_hash_type");
            case PREAUTH -> new SigningProfile(StandardCharsets.UTF_8, false, false, true,
                    "vnp_secure_hash", "vnp_secure_hash_type", "vnp_SecureHash", "vnp_SecureHashType");
            case PAYMENTLINK -> new SigningProfile(StandardCharsets.UTF_8, true, false, false,
                    "vnp_SecureHash", "vnp_SecureHashType");
        };
    }

    private static String buildHashData(Map<String, String> params, SigningProfile profile) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (isHashField(fieldName, profile.hashFields)) {
                continue;
            }
            if (profile.vnpOnly && (fieldName == null || !fieldName.toLowerCase().startsWith("vnp_"))) {
                continue;
            }
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            String valuePart = profile.encodeValues
                    ? URLEncoder.encode(fieldValue, profile.charset)
                    : fieldValue;
            if (profile.encodeFieldName) {
                hashData.append(URLEncoder.encode(fieldName, profile.charset))
                        .append('=')
                        .append(valuePart)
                        .append('&');
            } else {
                hashData.append(fieldName).append('=').append(valuePart).append('&');
            }
        }
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        return hashData.toString();
    }

    private static boolean isHashField(String fieldName, String... hashFields) {
        if (fieldName == null) {
            return true;
        }
        for (String hashField : hashFields) {
            if (fieldName.equalsIgnoreCase(hashField)) {
                return true;
            }
        }
        return false;
    }

    private record SigningProfile(
            Charset charset,
            boolean encodeFieldName,
            boolean encodeValues,
            boolean vnpOnly,
            String... hashFields
    ) {
    }
}

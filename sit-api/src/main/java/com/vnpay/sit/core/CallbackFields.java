package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;

public final class CallbackFields {

    private CallbackFields() {
    }

    public static boolean usesPascalCase(PaymentFlow flow) {
        return flow == PaymentFlow.PAY || flow == PaymentFlow.INSTALMENT;
    }

    public static String txnRefKey(PaymentFlow flow) {
        return usesPascalCase(flow) ? "vnp_TxnRef" : "vnp_txn_ref";
    }

    public static String tmnCodeKey(PaymentFlow flow) {
        return usesPascalCase(flow) ? "vnp_TmnCode" : "vnp_tmn_code";
    }

    public static String amountKey(PaymentFlow flow) {
        return usesPascalCase(flow) ? "vnp_Amount" : "vnp_amount";
    }

    public static String secureHashKey(PaymentFlow flow) {
        return usesPascalCase(flow) ? "vnp_SecureHash" : "vnp_secure_hash";
    }

}

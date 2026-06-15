package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public final class CallbackParamBuilder {

    private CallbackParamBuilder() {
    }

    public static Map<String, String> build(
            PaymentFlow flow,
            TestCaseType testCase,
            String tmnCode,
            String txnRef,
            long amountVnd,
            Long wrongAmountVnd
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        String now = formatPayDate();
        long amountMinor = amountVnd * 100;

        switch (flow) {
            case PAY -> {
                params.put("vnp_TmnCode", tmnCode);
                params.put("vnp_TxnRef", txnRef);
                params.put("vnp_Amount", String.valueOf(amountMinor));
                params.put("vnp_ResponseCode", responseCode(testCase));
                params.put("vnp_TransactionStatus", transactionStatus(testCase));
                params.put("vnp_TransactionNo", randomTxnNo());
                params.put("vnp_PayDate", now);
                params.put("vnp_BankCode", "NCB");
                params.put("vnp_CardType", "ATM");
                params.put("vnp_OrderInfo", "SIT test " + txnRef);
            }
            case TOKEN -> {
                params.put("vnp_tmn_code", tmnCode);
                params.put("vnp_txn_ref", txnRef);
                params.put("vnp_amount", String.valueOf(amountMinor));
                params.put("vnp_response_code", responseCode(testCase));
                params.put("vnp_transaction_status", transactionStatus(testCase));
                params.put("vnp_transaction_no", randomTxnNo());
                params.put("vnp_pay_date", now);
                params.put("vnp_command", "token_pay");
                if (testCase == TestCaseType.SUCCESS) {
                    params.put("vnp_token", "SIT_TOKEN_" + txnRef);
                    params.put("vnp_card_number", "411111****1111");
                }
            }
            case RECURRING -> {
                params.put("vnp_tmn_code", tmnCode);
                params.put("vnp_txn_ref", txnRef);
                params.put("vnp_amount", String.valueOf(amountMinor));
                params.put("vnp_response_code", responseCode(testCase));
                params.put("vnp_transaction_status", transactionStatus(testCase));
                params.put("vnp_transaction_no", randomTxnNo());
                params.put("vnp_pay_date", now);
                if (testCase == TestCaseType.SUCCESS) {
                    params.put("vnp_recurring_id", "REC_" + txnRef);
                }
            }
            case INSTALMENT -> {
                params.put("vnp_tmn_code", tmnCode);
                params.put("vnp_txn_ref", txnRef);
                params.put("vnp_amount", String.valueOf(amountMinor));
                params.put("vnp_response_code", responseCode(testCase));
                params.put("vnp_transaction_status", transactionStatus(testCase));
                params.put("vnp_transaction_no", randomTxnNo());
                params.put("vnp_pay_date", now);
                params.put("vnp_installment_term", "3");
            }
        }

        if (testCase == TestCaseType.ORDER_NOT_FOUND) {
            replaceTxnRef(params, flow, "SIT_NOTFOUND_" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (testCase == TestCaseType.WRONG_AMOUNT && wrongAmountVnd != null) {
            putAmount(params, flow, wrongAmountVnd * 100);
        }

        return params;
    }

    private static String responseCode(TestCaseType testCase) {
        return testCase == TestCaseType.FAILED ? "24" : "00";
    }

    private static String transactionStatus(TestCaseType testCase) {
        return testCase == TestCaseType.FAILED ? "02" : "00";
    }

    private static void replaceTxnRef(Map<String, String> params, PaymentFlow flow, String txnRef) {
        if (flow == PaymentFlow.PAY) {
            params.put("vnp_TxnRef", txnRef);
        } else {
            params.put("vnp_txn_ref", txnRef);
        }
    }

    private static void putAmount(Map<String, String> params, PaymentFlow flow, long amountMinor) {
        if (flow == PaymentFlow.PAY) {
            params.put("vnp_Amount", String.valueOf(amountMinor));
        } else {
            params.put("vnp_amount", String.valueOf(amountMinor));
        }
    }

    private static String randomTxnNo() {
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L);
    }

    private static String formatPayDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));
        return formatter.format(new Date());
    }
}

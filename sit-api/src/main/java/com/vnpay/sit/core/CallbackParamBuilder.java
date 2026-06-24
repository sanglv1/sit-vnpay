package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.RecurringIpnCommand;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.model.TokenIpnCommand;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public final class CallbackParamBuilder {

    private static final String SIT_APP_USER_ID = "SIT_USER";

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
        return build(flow, testCase, tmnCode, txnRef, amountVnd, wrongAmountVnd, null, null);
    }

    public static Map<String, String> build(
            PaymentFlow flow,
            TestCaseType testCase,
            String tmnCode,
            String txnRef,
            long amountVnd,
            Long wrongAmountVnd,
            RecurringIpnCommand recurringCommand
    ) {
        return build(flow, testCase, tmnCode, txnRef, amountVnd, wrongAmountVnd, recurringCommand, null);
    }

    public static Map<String, String> build(
            PaymentFlow flow,
            TestCaseType testCase,
            String tmnCode,
            String txnRef,
            long amountVnd,
            Long wrongAmountVnd,
            RecurringIpnCommand recurringCommand,
            TokenIpnCommand tokenCommand
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        String now = formatPayDate();
        long amountMinor = amountVnd * 100;

        switch (flow) {
            case PAY -> putPascalCaseIpnFields(params, tmnCode, txnRef, amountMinor, now, testCase, "NCB", "ATM");
            case INSTALMENT -> putPascalCaseIpnFields(params, tmnCode, txnRef, amountMinor, now, testCase, "VISA", null);
            case TOKEN -> {
                putSnakeCaseIpnFields(params, tmnCode, txnRef, amountMinor, now, testCase);
                putTokenIpnFields(params, txnRef, testCase, tokenCommand);
            }
            case RECURRING -> {
                putSnakeCaseIpnFields(params, tmnCode, txnRef, amountMinor, now, testCase);
                putRecurringIpnFields(params, txnRef, testCase, recurringCommand);
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

    private static void putPascalCaseIpnFields(
            Map<String, String> params,
            String tmnCode,
            String txnRef,
            long amountMinor,
            String now,
            TestCaseType testCase,
            String bankCode,
            String cardType
    ) {
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", String.valueOf(amountMinor));
        params.put("vnp_ResponseCode", responseCode(testCase));
        params.put("vnp_TransactionStatus", transactionStatus(testCase));
        params.put("vnp_TransactionNo", randomTxnNo());
        params.put("vnp_PayDate", now);
        params.put("vnp_BankCode", bankCode);
        params.put("vnp_OrderInfo", "SIT test " + txnRef);
        if (cardType != null) {
            params.put("vnp_CardType", cardType);
        }
    }

    private static void putSnakeCaseIpnFields(
            Map<String, String> params,
            String tmnCode,
            String txnRef,
            long amountMinor,
            String now,
            TestCaseType testCase
    ) {
        params.put("vnp_tmn_code", tmnCode);
        params.put("vnp_txn_ref", txnRef);
        params.put("vnp_amount", String.valueOf(amountMinor));
        params.put("vnp_response_code", responseCode(testCase));
        params.put("vnp_transaction_status", transactionStatus(testCase));
        params.put("vnp_transaction_no", randomTxnNo());
        params.put("vnp_pay_date", now);
    }

    private static void putSnakeCaseCommandFields(Map<String, String> params, String txnRef, String command) {
        params.put("vnp_command", command);
        params.put("vnp_app_user_id", SIT_APP_USER_ID);
        params.put("vnp_txn_desc", "SIT test " + txnRef);
        params.put("vnp_curr_code", "VND");
    }

    private static void putTokenIpnFields(
            Map<String, String> params,
            String txnRef,
            TestCaseType testCase,
            TokenIpnCommand tokenCommand
    ) {
        TokenIpnCommand command = tokenCommand != null ? tokenCommand : TokenIpnCommand.defaultForIpnSuite();
        putSnakeCaseCommandFields(params, txnRef, command.getCommandValue());
        if (testCase == TestCaseType.SUCCESS) {
            params.put("vnp_token", "SIT_TOKEN_" + txnRef);
            if (command != TokenIpnCommand.TOKEN_REMOVE) {
                params.put("vnp_card_number", "411111****1111");
            }
        }
    }

    private static void putRecurringIpnFields(
            Map<String, String> params,
            String txnRef,
            TestCaseType testCase,
            RecurringIpnCommand recurringCommand
    ) {
        RecurringIpnCommand command = recurringCommand != null
                ? recurringCommand
                : RecurringIpnCommand.defaultForIpnSuite();
        params.put("vnp_command", command.getCommandValue());
        params.put("vnp_app_user_id", SIT_APP_USER_ID);
        params.put("vnp_order_info", "SIT test " + txnRef);
        params.put("vnp_curr_code", "VND");
        if (testCase == TestCaseType.SUCCESS) {
            params.put("vnp_bank_code", "VISA");
            params.put("vnp_bank_tran_no", String.valueOf(System.currentTimeMillis()) + "001");
            params.put("vnp_card_number", "445653xxxxxx1096");
            params.put("vnp_card_type", "ATM");
            params.put("vnp_token", "SIT_TOKEN_" + txnRef);
            params.put("vnp_token_exp_date", formatTokenExpDate());
        }
    }

    private static String formatTokenExpDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        calendar.add(Calendar.YEAR, 1);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        formatter.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));
        return formatter.format(calendar.getTime());
    }

    private static String responseCode(TestCaseType testCase) {
        return testCase == TestCaseType.FAILED ? "24" : "00";
    }

    private static String transactionStatus(TestCaseType testCase) {
        return testCase == TestCaseType.FAILED ? "02" : "00";
    }

    private static void replaceTxnRef(Map<String, String> params, PaymentFlow flow, String txnRef) {
        params.put(CallbackFields.txnRefKey(flow), txnRef);
    }

    private static void putAmount(Map<String, String> params, PaymentFlow flow, long amountMinor) {
        params.put(CallbackFields.amountKey(flow), String.valueOf(amountMinor));
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

package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackParamBuilderTest {

    private static final Pattern PAY_DATE_PATTERN = Pattern.compile("^\\d{14}$");

    @Test
    void pay_success_shouldUsePascalCaseFieldsAndMinorAmount() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.SUCCESS, "TMN01", "PAY001", 100_000L, null);

        assertThat(params)
                .containsEntry("vnp_TmnCode", "TMN01")
                .containsEntry("vnp_TxnRef", "PAY001")
                .containsEntry("vnp_Amount", "10000000")
                .containsEntry("vnp_ResponseCode", "00")
                .containsEntry("vnp_TransactionStatus", "00")
                .containsEntry("vnp_BankCode", "NCB")
                .containsEntry("vnp_CardType", "ATM")
                .containsEntry("vnp_OrderInfo", "SIT test PAY001");
        assertThat(params.get("vnp_PayDate")).matches(PAY_DATE_PATTERN);
        assertThat(params.get("vnp_TransactionNo")).isNotBlank();
    }

    @Test
    void pay_failed_shouldUseFailureCodes() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.FAILED, "TMN01", "PAY002", 50_000L, null);

        assertThat(params)
                .containsEntry("vnp_ResponseCode", "24")
                .containsEntry("vnp_TransactionStatus", "02")
                .containsEntry("vnp_Amount", "5000000");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentFlow.class, names = {"TOKEN", "RECURRING", "INSTALMENT"})
    void nonPayFlows_shouldUseSnakeCaseFields(PaymentFlow flow) {
        Map<String, String> params = CallbackParamBuilder.build(
                flow, TestCaseType.SUCCESS, "TMN02", "TXN100", 200_000L, null);

        assertThat(params)
                .containsEntry("vnp_tmn_code", "TMN02")
                .containsEntry("vnp_txn_ref", "TXN100")
                .containsEntry("vnp_amount", "20000000")
                .containsEntry("vnp_response_code", "00")
                .containsEntry("vnp_transaction_status", "00");
        assertThat(params).doesNotContainKeys("vnp_TmnCode", "vnp_TxnRef", "vnp_Amount");
    }

    @Test
    void token_success_shouldIncludeTokenFields() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.SUCCESS, "TMN03", "TOK001", 75_000L, null);

        assertThat(params)
                .containsEntry("vnp_command", "token_pay")
                .containsEntry("vnp_token", "SIT_TOKEN_TOK001")
                .containsEntry("vnp_card_number", "411111****1111");
    }

    @Test
    void token_nonSuccess_shouldOmitTokenFields() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.INVALID_HASH, "TMN03", "TOK002", 75_000L, null);

        assertThat(params).doesNotContainKeys("vnp_token", "vnp_card_number");
    }

    @Test
    void recurring_success_shouldIncludeRecurringId() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING, TestCaseType.SUCCESS, "TMN04", "REC001", 300_000L, null);

        assertThat(params).containsEntry("vnp_recurring_id", "REC_REC001");
    }

    @Test
    void instalment_shouldIncludeTerm() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.INSTALMENT, TestCaseType.SUCCESS, "TMN05", "INS001", 1_000_000L, null);

        assertThat(params).containsEntry("vnp_installment_term", "3");
    }

    @Test
    void orderNotFound_shouldReplaceTxnRefWithGeneratedValue() {
        Map<String, String> payParams = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG001", 100_000L, null);
        Map<String, String> tokenParams = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG002", 100_000L, null);

        assertThat(payParams.get("vnp_TxnRef")).startsWith("SIT_NOTFOUND_").doesNotContain("ORIG001");
        assertThat(tokenParams.get("vnp_txn_ref")).startsWith("SIT_NOTFOUND_").doesNotContain("ORIG002");
    }

    @Test
    void wrongAmount_shouldOverrideAmountInMinorUnits() {
        Map<String, String> payParams = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.WRONG_AMOUNT, "TMN01", "PAY003", 100_000L, 99_000L);
        Map<String, String> tokenParams = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.WRONG_AMOUNT, "TMN01", "TOK003", 100_000L, 99_000L);

        assertThat(payParams).containsEntry("vnp_Amount", "9900000");
        assertThat(tokenParams).containsEntry("vnp_amount", "9900000");
    }

    @Test
    void wrongAmount_withoutOverride_shouldKeepOriginalAmount() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.WRONG_AMOUNT, "TMN01", "PAY004", 100_000L, null);

        assertThat(params).containsEntry("vnp_Amount", "10000000");
    }
}

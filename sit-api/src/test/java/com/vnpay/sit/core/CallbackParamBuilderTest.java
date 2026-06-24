package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.RecurringIpnCommand;
import com.vnpay.sit.model.TestCaseType;
import com.vnpay.sit.model.TokenIpnCommand;
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

    @Test
    void instalment_success_shouldUsePascalCaseRequiredFields() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.INSTALMENT, TestCaseType.SUCCESS, "TMN05", "INS001", 1_000_000L, null);

        assertThat(params)
                .containsEntry("vnp_TmnCode", "TMN05")
                .containsEntry("vnp_TxnRef", "INS001")
                .containsEntry("vnp_Amount", "100000000")
                .containsEntry("vnp_ResponseCode", "00")
                .containsEntry("vnp_TransactionStatus", "00")
                .containsEntry("vnp_BankCode", "VISA")
                .containsEntry("vnp_OrderInfo", "SIT test INS001");
        assertThat(params).doesNotContainKeys(
                "vnp_tmn_code", "vnp_txn_ref", "vnp_amount", "vnp_installment_term");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentFlow.class, names = {"TOKEN"})
    void snakeCaseFlows_shouldUseSnakeCaseCoreFields(PaymentFlow flow) {
        Map<String, String> params = CallbackParamBuilder.build(
                flow, TestCaseType.SUCCESS, "TMN02", "TXN100", 200_000L, null);

        assertThat(params)
                .containsEntry("vnp_tmn_code", "TMN02")
                .containsEntry("vnp_txn_ref", "TXN100")
                .containsEntry("vnp_amount", "20000000")
                .containsEntry("vnp_response_code", "00")
                .containsEntry("vnp_transaction_status", "00")
                .containsEntry("vnp_command", "pay_and_create")
                .containsEntry("vnp_app_user_id", "SIT_USER")
                .containsEntry("vnp_txn_desc", "SIT test TXN100")
                .containsEntry("vnp_curr_code", "VND");
        assertThat(params).doesNotContainKeys("vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_order_info");
    }

    @Test
    void recurring_success_defaultCommand_shouldMatchCardVerificationIpn() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING, TestCaseType.SUCCESS, "OREDTES4", "RECX4BXBF31F", 2_000_000L, null);

        assertThat(params)
                .containsEntry("vnp_tmn_code", "OREDTES4")
                .containsEntry("vnp_txn_ref", "RECX4BXBF31F")
                .containsEntry("vnp_amount", "200000000")
                .containsEntry("vnp_response_code", "00")
                .containsEntry("vnp_transaction_status", "00")
                .containsEntry("vnp_command", "recurring")
                .containsEntry("vnp_app_user_id", "SIT_USER")
                .containsEntry("vnp_order_info", "SIT test RECX4BXBF31F")
                .containsEntry("vnp_curr_code", "VND")
                .containsEntry("vnp_bank_code", "VISA")
                .containsEntry("vnp_card_number", "445653xxxxxx1096")
                .containsEntry("vnp_card_type", "ATM")
                .containsEntry("vnp_token", "SIT_TOKEN_RECX4BXBF31F");
        assertThat(params.get("vnp_token_exp_date")).matches("\\d{8}");
        assertThat(params.get("vnp_bank_tran_no")).isNotBlank();
        assertThat(params.get("vnp_pay_date")).matches(PAY_DATE_PATTERN);
        assertThat(params).doesNotContainKeys("vnp_txn_desc", "vnp_recurring_id");
    }

    @ParameterizedTest
    @EnumSource(RecurringIpnCommand.class)
    void recurring_success_shouldSupportAllVnPayCommands(RecurringIpnCommand command) {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING,
                TestCaseType.SUCCESS,
                "TMN04",
                "REC001",
                300_000L,
                null,
                command);

        assertThat(params)
                .containsEntry("vnp_command", command.getCommandValue())
                .containsEntry("vnp_app_user_id", "SIT_USER")
                .containsEntry("vnp_order_info", "SIT test REC001")
                .containsEntry("vnp_curr_code", "VND")
                .containsEntry("vnp_token", "SIT_TOKEN_REC001");
    }

    @ParameterizedTest
    @EnumSource(RecurringIpnCommand.class)
    void recurring_nonSuccess_shouldOmitOptionalCardAndTokenFields(RecurringIpnCommand command) {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING, TestCaseType.INVALID_HASH, "TMN04", "REC002", 300_000L, null, command);

        assertThat(params).doesNotContainKeys(
                "vnp_token", "vnp_token_exp_date", "vnp_card_number", "vnp_bank_code", "vnp_bank_tran_no");
        assertThat(params)
                .containsEntry("vnp_command", command.getCommandValue())
                .containsEntry("vnp_order_info", "SIT test REC002")
                .containsEntry("vnp_curr_code", "VND");
    }

    @Test
    void recurring_payNRecurring_success_shouldUseExplicitCommand() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING,
                TestCaseType.SUCCESS,
                "TMN04",
                "REC001",
                300_000L,
                null,
                RecurringIpnCommand.PAY_N_RECURRING);

        assertThat(params).containsEntry("vnp_command", "pay_n_recurring");
    }

    @Test
    void recurring_updateToken_success_shouldUseExplicitCommand() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING,
                TestCaseType.SUCCESS,
                "TMN04",
                "REC001",
                300_000L,
                null,
                RecurringIpnCommand.UPDATE_TOKEN);

        assertThat(params).containsEntry("vnp_command", "update_token");
    }

    @Test
    void token_success_defaultCommand_shouldMatchPayAndCreateIpn() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.SUCCESS, "TMN03", "TOK001", 75_000L, null);

        assertThat(params)
                .containsEntry("vnp_command", "pay_and_create")
                .containsEntry("vnp_token", "SIT_TOKEN_TOK001")
                .containsEntry("vnp_card_number", "411111****1111")
                .containsEntry("vnp_app_user_id", "SIT_USER")
                .containsEntry("vnp_txn_desc", "SIT test TOK001")
                .containsEntry("vnp_curr_code", "VND");
    }

    @ParameterizedTest
    @EnumSource(TokenIpnCommand.class)
    void token_success_shouldSupportAllVnPayCommands(TokenIpnCommand command) {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN,
                TestCaseType.SUCCESS,
                "TMN03",
                "TOK001",
                75_000L,
                null,
                null,
                command);

        assertThat(params)
                .containsEntry("vnp_command", command.getCommandValue())
                .containsEntry("vnp_token", "SIT_TOKEN_TOK001");
        if (command != TokenIpnCommand.TOKEN_REMOVE) {
            assertThat(params).containsEntry("vnp_card_number", "411111****1111");
        } else {
            assertThat(params).doesNotContainKey("vnp_card_number");
        }
    }

    @ParameterizedTest
    @EnumSource(TokenIpnCommand.class)
    void token_nonSuccess_shouldOmitOptionalTokenFields(TokenIpnCommand command) {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.INVALID_HASH, "TMN03", "TOK002", 75_000L, null, null, command);

        assertThat(params).doesNotContainKeys("vnp_token", "vnp_card_number");
        assertThat(params)
                .containsEntry("vnp_command", command.getCommandValue())
                .containsEntry("vnp_app_user_id", "SIT_USER")
                .containsEntry("vnp_curr_code", "VND");
    }

    @Test
    void token_pay_success_shouldUseExplicitCommand() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN,
                TestCaseType.SUCCESS,
                "TMN03",
                "TOK001",
                75_000L,
                null,
                null,
                TokenIpnCommand.TOKEN_PAY);

        assertThat(params).containsEntry("vnp_command", "token_pay");
    }

    @ParameterizedTest
    @EnumSource(TokenIpnCommand.class)
    void token_wrongAmount_shouldOverrideAmountForAllCommands(TokenIpnCommand command) {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.WRONG_AMOUNT, "TMN01", "TOK003", 100_000L, 99_000L, null, command);

        assertThat(params)
                .containsEntry("vnp_amount", "9900000")
                .containsEntry("vnp_command", command.getCommandValue());
    }


    @ParameterizedTest
    @EnumSource(RecurringIpnCommand.class)
    void recurring_wrongAmount_shouldOverrideAmountForAllCommands(RecurringIpnCommand command) {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.RECURRING, TestCaseType.WRONG_AMOUNT, "TMN01", "REC003", 100_000L, 99_000L, command);

        assertThat(params)
                .containsEntry("vnp_amount", "9900000")
                .containsEntry("vnp_command", command.getCommandValue());
    }

    @Test
    void orderNotFound_shouldReplaceTxnRefWithGeneratedValue() {
        Map<String, String> payParams = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG001", 100_000L, null);
        Map<String, String> tokenParams = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG002", 100_000L, null);
        Map<String, String> recurringParams = CallbackParamBuilder.build(
                PaymentFlow.RECURRING, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG004", 100_000L, null);
        Map<String, String> instalmentParams = CallbackParamBuilder.build(
                PaymentFlow.INSTALMENT, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG003", 100_000L, null);

        assertThat(payParams.get("vnp_TxnRef")).startsWith("SIT_NOTFOUND_").doesNotContain("ORIG001");
        assertThat(tokenParams.get("vnp_txn_ref")).startsWith("SIT_NOTFOUND_").doesNotContain("ORIG002");
        assertThat(recurringParams.get("vnp_txn_ref")).startsWith("SIT_NOTFOUND_").doesNotContain("ORIG004");
        assertThat(instalmentParams.get("vnp_TxnRef")).startsWith("SIT_NOTFOUND_").doesNotContain("ORIG003");
    }

    @Test
    void wrongAmount_shouldOverrideAmountInMinorUnits() {
        Map<String, String> payParams = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.WRONG_AMOUNT, "TMN01", "PAY003", 100_000L, 99_000L);
        Map<String, String> tokenParams = CallbackParamBuilder.build(
                PaymentFlow.TOKEN, TestCaseType.WRONG_AMOUNT, "TMN01", "TOK003", 100_000L, 99_000L);
        Map<String, String> recurringParams = CallbackParamBuilder.build(
                PaymentFlow.RECURRING, TestCaseType.WRONG_AMOUNT, "TMN01", "REC003", 100_000L, 99_000L);
        Map<String, String> instalmentParams = CallbackParamBuilder.build(
                PaymentFlow.INSTALMENT, TestCaseType.WRONG_AMOUNT, "TMN01", "INS003", 100_000L, 99_000L);

        assertThat(payParams).containsEntry("vnp_Amount", "9900000");
        assertThat(tokenParams).containsEntry("vnp_amount", "9900000");
        assertThat(recurringParams).containsEntry("vnp_amount", "9900000");
        assertThat(instalmentParams).containsEntry("vnp_Amount", "9900000");
    }

    @Test
    void wrongAmount_withoutOverride_shouldKeepOriginalAmount() {
        Map<String, String> params = CallbackParamBuilder.build(
                PaymentFlow.PAY, TestCaseType.WRONG_AMOUNT, "TMN01", "PAY004", 100_000L, null);

        assertThat(params).containsEntry("vnp_Amount", "10000000");
    }
}

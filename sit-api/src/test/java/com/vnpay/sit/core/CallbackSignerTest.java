package com.vnpay.sit.core;

import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.model.TestCaseType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackSignerTest {

  private static final String SECRET = "TEST_SECRET_KEY_12345";

  @Test
  void pay_sign_shouldBeDeterministic() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.PAY, TestCaseType.SUCCESS, "TMN01", "PAY001", 100_000L, null);
    String hash1 = CallbackSigner.sign(params, SECRET, PaymentFlow.PAY);
    String hash2 = CallbackSigner.sign(params, SECRET, PaymentFlow.PAY);
    assertThat(hash1).isNotBlank().hasSize(128).isEqualTo(hash2);
  }

  @Test
  void token_sign_shouldUseSnakeCaseHashField() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.TOKEN, TestCaseType.SUCCESS, "TMN01", "TOK001", 50_000L, null);
    CallbackSigner.attachHash(params, SECRET, PaymentFlow.TOKEN);
    assertThat(params).containsKey("vnp_secure_hash");
    assertThat(params.get("vnp_secure_hash")).hasSize(128);
  }

  @Test
  void invalidHash_shouldDifferFromValid() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.RECURRING, TestCaseType.SUCCESS, "TMN01", "REC001", 200_000L, null);
    String valid = CallbackSigner.sign(params, SECRET, PaymentFlow.RECURRING);
    String invalid = CallbackSigner.sign(params, "WRONG_KEY", PaymentFlow.RECURRING);
    assertThat(valid).isNotEqualTo(invalid);
  }

  @Test
  void pay_hashFieldName() {
    assertThat(CallbackSigner.hashFieldFor(PaymentFlow.PAY)).isEqualTo("vnp_SecureHash");
    assertThat(CallbackSigner.hashFieldFor(PaymentFlow.TOKEN)).isEqualTo("vnp_secure_hash");
  }

  @Test
  void orderNotFound_shouldChangeTxnRef() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.PAY, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG001", 100_000L, null);
    assertThat(params.get("vnp_TxnRef")).doesNotContain("ORIG001");
  }
}

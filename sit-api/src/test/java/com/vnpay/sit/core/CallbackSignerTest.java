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
    assertThat(CallbackSigner.hashFieldFor(PaymentFlow.INSTALMENT)).isEqualTo("vnp_SecureHash");
    assertThat(CallbackSigner.hashFieldFor(PaymentFlow.TOKEN)).isEqualTo("vnp_secure_hash");
  }

  @Test
  void instalment_sign_shouldUsePascalCaseHashField() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.INSTALMENT, TestCaseType.SUCCESS, "TMN01", "INS001", 100_000L, null);
    CallbackSigner.attachHash(params, SECRET, PaymentFlow.INSTALMENT);
    assertThat(params).containsKey("vnp_SecureHash");
    assertThat(params.get("vnp_SecureHash")).hasSize(128);
  }

  @Test
  void preAuth_sign_shouldUseSnakeCaseHashFieldWithoutUrlEncoding() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.PREAUTH, TestCaseType.SUCCESS, "TMN01", "PA001", 100_000L, null);
    CallbackSigner.attachHash(params, SECRET, PaymentFlow.PREAUTH);
    assertThat(params).containsKey("vnp_secure_hash");
    assertThat(params.get("vnp_secure_hash")).hasSize(128);
    // Raw signing: same params + key should produce same hash
    String hash1 = CallbackSigner.sign(params, SECRET, PaymentFlow.PREAUTH);
    String hash2 = CallbackSigner.sign(params, SECRET, PaymentFlow.PREAUTH);
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void qrDirect_sign_shouldUsePascalCaseHashField() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.QR_DIRECT, TestCaseType.SUCCESS, "TMN01", "QR001", 100_000L, null);
    CallbackSigner.attachHash(params, SECRET, PaymentFlow.QR_DIRECT);
    assertThat(params).containsKey("vnp_SecureHash");
  }

  @Test
  void orderNotFound_shouldChangeTxnRef() {
    Map<String, String> params = CallbackParamBuilder.build(
        PaymentFlow.PAY, TestCaseType.ORDER_NOT_FOUND, "TMN01", "ORIG001", 100_000L, null);
    assertThat(params.get("vnp_TxnRef")).doesNotContain("ORIG001");
  }
}

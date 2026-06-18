package com.vnpay.sit.session.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveSessionTestInputForm {

    private String pendingTxnRef;

    private Long pendingAmountVnd;

    private String confirmedTxnRef;

    private Long confirmedAmountVnd;

    private String failedTxnRef;

    private Long failedAmountVnd;

    private Long wrongAmountVnd;
}

package com.vnpay.sit.session.dto;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonAlias;

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

    /** Chỉ áp dụng luồng RECURRING; chuỗi rỗng → xóa giá trị đã lưu. */
    @JsonAlias({"appUserId", "vnp_app_user_id"})
    private String recurringAppUserId;
}

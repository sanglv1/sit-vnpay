package com.vnpay.sit.manual.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualAcceptanceForm {

    private Long id;

    @NotNull(message = "Chọn đối tác")
    private Long partnerId;

    private Long sessionId;

    private String returnSuccessTxnRef;
    private String returnSuccessImage;
    private String returnFailedTxnRef;
    private String returnFailedImage;

    /** true = Đã xử lý, false = Chưa xử lý */
    private Boolean exceptionHandled;
    /** true = Đạt, false = Không đạt */
    private Boolean whitelistIpPassed;
    /** true = Đạt, false = Không đạt */
    private Boolean logStoragePassed;

    private String note;
}

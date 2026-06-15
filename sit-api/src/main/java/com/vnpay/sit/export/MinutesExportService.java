package com.vnpay.sit.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.manual.entity.ManualAcceptance;
import com.vnpay.sit.manual.service.ManualAcceptanceService;
import com.vnpay.sit.model.CallbackType;
import com.vnpay.sit.model.PaymentFlow;
import com.vnpay.sit.partner.entity.PartnerConfig;
import com.vnpay.sit.partner.service.PartnerService;
import com.vnpay.sit.session.entity.TestSession;
import com.vnpay.sit.session.service.TestSessionService;
import com.vnpay.sit.testrun.entity.TestRun;
import com.vnpay.sit.testrun.repository.TestRunRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class MinutesExportService {

    private final TestSessionService testSessionService;
    private final PartnerService partnerService;
    private final TestRunRepository testRunRepository;
    private final ManualAcceptanceService manualAcceptanceService;
    private final MinutesDocumentFiller documentFiller;

    public MinutesExportService(
            TestSessionService testSessionService,
            PartnerService partnerService,
            TestRunRepository testRunRepository,
            ManualAcceptanceService manualAcceptanceService,
            ObjectMapper objectMapper
    ) {
        this.testSessionService = testSessionService;
        this.partnerService = partnerService;
        this.testRunRepository = testRunRepository;
        this.manualAcceptanceService = manualAcceptanceService;
        this.documentFiller = new MinutesDocumentFiller(objectMapper);
    }

    public ExportedMinutes export(
            Long sessionId,
            String vnpayRepresentative,
            String merchantRepresentative,
            String websiteName,
            String testLink,
            String integrationVersion
    ) {
        TestSession session = testSessionService.findEntityById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên kiểm thử"));

        PartnerConfig partner = partnerService.findById(session.getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đối tác"));

        List<TestRun> runs = testRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .filter(run -> run.getCallbackType() == CallbackType.IPN)
                .toList();

        ManualAcceptance manual = manualAcceptanceService.findLatestBySession(sessionId).orElse(null);

        MinutesExportContext context = MinutesExportContext.builder()
                .session(session)
                .partner(partner)
                .manualAcceptance(manual)
                .ipnRuns(MinutesExportContext.indexLatestIpnRuns(runs))
                .vnpayRepresentative(vnpayRepresentative)
                .merchantRepresentative(merchantRepresentative)
                .websiteName(websiteName)
                .testLink(testLink)
                .integrationVersion(integrationVersion)
                .build();

        try (InputStream inputStream = templateResource(partner.getFlow()).getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            documentFiller.fill(document, context);
            document.write(outputStream);
            return new ExportedMinutes(buildFileName(partner.getTmnCode()), outputStream.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xuất biên bản: " + ex.getMessage(), ex);
        }
    }

    private ClassPathResource templateResource(PaymentFlow flow) {
        String fileName = switch (flow) {
            case PAY -> "VNPAYGW-Pay-SIT-VN.docx";
            case TOKEN -> "VNPAYGW-Token-SIT-VN.docx";
            case RECURRING -> "VNPAYGW-Recurring-SIT-VN.docx";
            case INSTALMENT -> "VNPAYGW-Installment-SIT-VN.docx";
        };
        return new ClassPathResource("templates/minutes/" + fileName);
    }

    public static String buildFileName(String tmnCode) {
        String safeCode = tmnCode == null ? "UNKNOWN" : tmnCode.replaceAll("[^A-Za-z0-9_-]", "");
        return "VNPAYGW-" + safeCode + "-SIT.docx";
    }

    public record ExportedMinutes(String fileName, byte[] content) {
    }
}

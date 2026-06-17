package com.vnpay.sit.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vnpay.sit.model.PaymentFlow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

final class MinutesDocumentFiller {

    private final PayMinutesDocumentFiller payFiller;
    private final InstalmentMinutesDocumentFiller instalmentFiller;
    private final TokenRecurringMinutesDocumentFiller tokenRecurringFiller;
    private final MinutesViewModelMapper viewModelMapper;

    MinutesDocumentFiller(ObjectMapper objectMapper, MinutesViewModelMapper viewModelMapper) {
        this.viewModelMapper = viewModelMapper;
        this.payFiller = new PayMinutesDocumentFiller(objectMapper, viewModelMapper);
        this.instalmentFiller = new InstalmentMinutesDocumentFiller(objectMapper, viewModelMapper);
        this.tokenRecurringFiller = new TokenRecurringMinutesDocumentFiller(objectMapper, viewModelMapper);
    }

    void fill(XWPFDocument document, MinutesExportContext ctx) {
        applyTemplateTokens(document, viewModelMapper.map(ctx));
        switch (ctx.flow()) {
            case PAY -> payFiller.fill(document, ctx);
            case INSTALMENT -> instalmentFiller.fill(document, ctx);
            case TOKEN, RECURRING -> tokenRecurringFiller.fill(document, ctx);
        }
    }

    private void applyTemplateTokens(XWPFDocument document, MinutesViewModelMapper.MinutesViewModel viewModel) {
        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String updated = text;
            for (var token : viewModel.tokens().entrySet()) {
                updated = updated.replace("${" + token.getKey() + "}", token.getValue());
            }
            if (!updated.equals(text)) {
                DocxParagraphWalker.setParagraphText(paragraph, updated);
            }
        }
    }
}

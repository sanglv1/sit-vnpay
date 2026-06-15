package com.vnpay.sit.export;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.List;

final class DocxParagraphWalker {

    private DocxParagraphWalker() {
    }

    static List<XWPFParagraph> allParagraphs(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = new ArrayList<>();
        collectBodyElements(document.getBodyElements(), paragraphs);
        document.getHeaderList().forEach(header -> paragraphs.addAll(header.getParagraphs()));
        document.getFooterList().forEach(footer -> paragraphs.addAll(footer.getParagraphs()));
        return paragraphs;
    }

    private static void collectBodyElements(List<IBodyElement> elements, List<XWPFParagraph> paragraphs) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                paragraphs.add(paragraph);
            } else if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collectBodyElements(cell.getBodyElements(), paragraphs);
                    }
                }
            }
        }
    }

    static void setParagraphText(XWPFParagraph paragraph, String text) {
        List<org.apache.poi.xwpf.usermodel.XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) {
            paragraph.createRun().setText(text, 0);
            return;
        }
        for (int i = runs.size() - 1; i > 0; i--) {
            paragraph.removeRun(i);
        }
        runs.get(0).setText(text, 0);
    }
}

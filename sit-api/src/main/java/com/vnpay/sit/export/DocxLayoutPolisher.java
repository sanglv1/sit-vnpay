package com.vnpay.sit.export;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.util.List;
import java.util.function.Consumer;

public final class DocxLayoutPolisher {

    private DocxLayoutPolisher() {
    }

    public static int countMerchantHeaderBlankLines(XWPFDocument document) {
        int[] blanks = {0};
        forEachMerchantHeaderCell(document, cell -> {
            boolean inSection = false;
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                String text = normalize(paragraph.getText());
                if (text.contains("Thông tin merchant kết nối")) {
                    inSection = true;
                    continue;
                }
                if (text.contains("Nhân sự")) {
                    break;
                }
                if (inSection && shouldRemoveMerchantHeaderLine(text)) {
                    blanks[0]++;
                }
            }
        });
        return blanks[0];
    }

    static void polish(XWPFDocument document) {
        polishEllipsisPlaceholders(document);
        compactMerchantHeader(document);
        polishEvaluationCells(document);
    }

    private static void polishEllipsisPlaceholders(XWPFDocument document) {
        for (XWPFParagraph paragraph : DocxParagraphWalker.allParagraphs(document)) {
            String text = normalize(paragraph.getText());
            if (text.isEmpty()) {
                continue;
            }
            if (isEllipsisOnly(text)) {
                DocxParagraphWalker.setParagraphText(paragraph, "");
                continue;
            }
            if (text.endsWith("…") || text.endsWith(": …")) {
                String cleaned = text.replaceAll("\\s*…+", "").trim();
                if (!cleaned.equals(text)) {
                    DocxParagraphWalker.setParagraphText(paragraph, cleaned);
                }
            }
        }
    }

    private static void compactMerchantHeader(XWPFDocument document) {
        forEachMerchantHeaderCell(document, DocxLayoutPolisher::compactMerchantHeaderInCell);
    }

    private static void forEachMerchantHeaderCell(XWPFDocument document, Consumer<XWPFTableCell> consumer) {
        DocxParagraphWalker.forEachTableCell(document, cell -> {
            if (cell.getText() != null && cell.getText().contains("Thông tin merchant kết nối")) {
                consumer.accept(cell);
            }
        });
    }

    private static void compactMerchantHeaderInCell(XWPFTableCell cell) {
        String cellText = cell.getText();
        if (cellText == null || !cellText.contains("Thông tin merchant kết nối")) {
            return;
        }
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        boolean inSection = false;
        for (int i = paragraphs.size() - 1; i >= 0; i--) {
            String text = normalize(paragraphs.get(i).getText());
            if (text.contains("Thông tin merchant kết nối")) {
                inSection = false;
                continue;
            }
            if (text.contains("Nhân sự thực hiện")) {
                inSection = true;
                continue;
            }
            if (inSection && shouldRemoveMerchantHeaderLine(text)) {
                cell.removeParagraph(i);
            }
        }
    }

    private static boolean shouldRemoveMerchantHeaderLine(String text) {
        return text.isEmpty()
                || text.startsWith("Link test:");
    }

    private static void polishEvaluationCells(XWPFDocument document) {
        DocxParagraphWalker.forEachTableRow(document, row -> {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 5) {
                return;
            }
            String evaluation = normalize(cells.get(4).getText());
            if (!evaluation.isEmpty()) {
                DocxParagraphWalker.styleEvaluationCell(cells.get(4));
            }
        });
    }

    private static boolean isEllipsisOnly(String text) {
        String trimmed = text.trim();
        return trimmed.equals("…")
                || trimmed.equals("...")
                || trimmed.equals("….")
                || trimmed.matches("[\\s….]+");
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replace('\t', ' ').replaceAll(" +", " ").trim();
    }
}

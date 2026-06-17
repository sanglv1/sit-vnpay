package com.vnpay.sit.export;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    static void setCellText(XWPFTableCell cell, String text) {
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        if (paragraphs.isEmpty()) {
            cell.addParagraph().createRun().setText(text, 0);
            return;
        }
        setParagraphText(paragraphs.get(0), text);
        for (int i = paragraphs.size() - 1; i > 0; i--) {
            cell.removeParagraph(i);
        }
    }

    static void styleEvaluationCell(XWPFTableCell cell) {
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            paragraph.setAlignment(ParagraphAlignment.CENTER);
        }
    }

    static void forEachTableCell(XWPFDocument document, Consumer<XWPFTableCell> consumer) {
        forEachTableCell(document.getBodyElements(), consumer);
    }

    private static void forEachTableCell(List<IBodyElement> elements, Consumer<XWPFTableCell> consumer) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        consumer.accept(cell);
                        forEachTableCell(cell.getBodyElements(), consumer);
                    }
                }
            }
        }
    }

    static void forEachTableRow(XWPFDocument document, TableRowConsumer consumer) {
        forEachTableRow(document.getBodyElements(), consumer);
    }

    private static void forEachTableRow(List<IBodyElement> elements, TableRowConsumer consumer) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    consumer.accept(row);
                    for (XWPFTableCell cell : row.getTableCells()) {
                        forEachTableRow(cell.getBodyElements(), consumer);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    interface TableRowConsumer {
        void accept(XWPFTableRow row);
    }
}

package com.vnpay.sit.export;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DocxImageInserter {
    private static final Logger log = LoggerFactory.getLogger(DocxImageInserter.class);
    private static final Pattern DATA_URL = Pattern.compile(
            "^data:image/(\\w+);base64,(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final int MAX_WIDTH_PX = 420;

    private DocxImageInserter() {
    }

    static boolean embedDataUrlImage(XWPFParagraph paragraph, String dataUrl, String caption) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return false;
        }
        try {
            DecodedImage image = decodeDataUrl(dataUrl.trim());
            clearRuns(paragraph);

            XWPFRun captionRun = paragraph.createRun();
            if (caption != null && !caption.isBlank()) {
                captionRun.setText(caption);
                captionRun.addBreak();
            }

            int[] emuSize = toEmuSize(image);
            XWPFRun imageRun = paragraph.createRun();
            imageRun.addPicture(
                    new ByteArrayInputStream(image.bytes()),
                    image.pictureType(),
                    "return-url-screenshot." + image.extension(),
                    emuSize[0],
                    emuSize[1]
            );
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            return true;
        } catch (Exception ex) {
            log.warn("Cannot embed return URL image into minutes document", ex);
            return false;
        }
    }

    private static void clearRuns(XWPFParagraph paragraph) {
        int runs = paragraph.getRuns().size();
        for (int i = runs - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
    }

    private static int[] toEmuSize(DecodedImage image) throws Exception {
        BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(image.bytes()));
        if (buffered == null) {
            int widthEmu = Units.pixelToEMU(MAX_WIDTH_PX);
            return new int[]{widthEmu, Units.pixelToEMU(MAX_WIDTH_PX * 3 / 4)};
        }
        int widthPx = buffered.getWidth();
        int heightPx = buffered.getHeight();
        if (widthPx > MAX_WIDTH_PX) {
            heightPx = Math.max(1, heightPx * MAX_WIDTH_PX / widthPx);
            widthPx = MAX_WIDTH_PX;
        }
        return new int[]{Units.pixelToEMU(widthPx), Units.pixelToEMU(heightPx)};
    }

    private static DecodedImage decodeDataUrl(String dataUrl) {
        Matcher matcher = DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported image data URL");
        }
        String format = matcher.group(1).toLowerCase();
        byte[] bytes = Base64.getDecoder().decode(matcher.group(2).replaceAll("\\s+", ""));
        return new DecodedImage(bytes, pictureType(format), format.equals("jpeg") ? "jpg" : format);
    }

    private static int pictureType(String format) {
        return switch (format) {
            case "png" -> Document.PICTURE_TYPE_PNG;
            case "jpg", "jpeg" -> Document.PICTURE_TYPE_JPEG;
            case "gif" -> Document.PICTURE_TYPE_GIF;
            case "bmp" -> Document.PICTURE_TYPE_BMP;
            default -> throw new IllegalArgumentException("Unsupported image format: " + format);
        };
    }

    private record DecodedImage(byte[] bytes, int pictureType, String extension) {
    }
}

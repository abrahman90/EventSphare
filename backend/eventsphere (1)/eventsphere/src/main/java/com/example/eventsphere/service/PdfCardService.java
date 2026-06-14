package com.example.eventsphere.service;

import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.InvitationCard;
import com.example.eventsphere.enums.EventLayout;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfCardService {

    @Value("${app.cards.dir:./generated-cards}")
    private String cardsDir;

    // Page: A5 landscape = 595 × 420 pt  (wider card, more room for QR)
    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 420f;

    public String generateInvitationCard(InvitationCard card, Event event) throws IOException {
        Path cardDir = Paths.get(cardsDir, "cards", String.valueOf(event.getId()));
        Files.createDirectories(cardDir);
        String filename = "card_" + card.getInvitationId() + ".pdf";
        Path pdfPath = cardDir.resolve(filename);

        try (PdfWriter writer   = new PdfWriter(pdfPath.toString());
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document doc       = new Document(pdfDoc, new PageSize(PAGE_W, PAGE_H))) {

            doc.setMargins(0, 0, 0, 0);
            LayoutColors c = getLayoutColors(event.getSelectedLayout());

            // ── Two-column table: 60% left (details) | 40% right (QR) ──────────
            Table root = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
            root.setWidth(UnitValue.createPercentValue(100));
            root.setHeight(PAGE_H);

            // ── LEFT CELL ───────────────────────────────────────────────────────
            Cell left = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBackgroundColor(c.primary)
                    .setPaddingLeft(32).setPaddingRight(24)
                    .setPaddingTop(28).setPaddingBottom(24)
                    .setVerticalAlignment(VerticalAlignment.TOP);

            // Brand
            left.add(new Paragraph("✦  EventSphere")
                    .setFontSize(9).setFontColor(c.lightText)
                    .setMarginBottom(10));

            // Event name — large & bold
            left.add(new Paragraph(event.getEventName())
                    .setFontSize(20).setBold()
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setMarginBottom(6));

            // Category badge
            if (event.getCategory() != null) {
                left.add(new Paragraph(event.getCategory().name())
                        .setFontSize(8).setBold()
                        .setFontColor(c.labelText)
                        .setMarginBottom(14));
            }

            // Divider
            LineSeparator sep = new LineSeparator(new SolidLine(0.5f));
            sep.setStrokeColor(c.lightText);
            sep.setMarginBottom(14);
            left.add(sep);

            // Detail rows
            left.add(detail("📍", "Venue",   event.getVenue(), c));
            left.add(detail("📅", "Date",    event.getEventDate().toString(), c));
            left.add(detail("🕐", "Time",    event.getStartTime() + " – " + event.getEndTime(), c));
            if (event.getContactInfo() != null && !event.getContactInfo().isBlank()) {
                left.add(detail("📞", "Contact", event.getContactInfo(), c));
            }

            // Description (truncated)
            if (event.getDescription() != null && !event.getDescription().isBlank()) {
                String desc = event.getDescription().length() > 130
                        ? event.getDescription().substring(0, 130) + "…"
                        : event.getDescription();
                left.add(new Paragraph(desc)
                        .setFontSize(8.5f).setItalic()
                        .setFontColor(c.lightText)
                        .setMarginTop(12));
            }

            root.addCell(left);

            // ── RIGHT CELL ──────────────────────────────────────────────────────
            Cell right = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBackgroundColor(c.secondary)
                    .setPadding(22)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER);

            right.add(new Paragraph("INVITATION")
                    .setFontSize(10).setBold()
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(16));

            // QR Code — large white-padded box
            if (card.getQrCodePath() != null) {
                try {
                    ImageData imgData = ImageDataFactory.create(card.getQrCodePath());
                    Image qrImg = new Image(imgData)
                            .setWidth(150).setHeight(150)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);

                    // White card around QR
                    Div qrBox = new Div()
                            .setBackgroundColor(new DeviceRgb(255, 255, 255))
                            .setPadding(10)
                            .setBorderRadius(new BorderRadius(10))
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
                    qrBox.add(qrImg);
                    right.add(qrBox);

                } catch (Exception e) {
                    right.add(new Paragraph("[QR Code]")
                            .setFontColor(c.lightText)
                            .setTextAlignment(TextAlignment.CENTER));
                }
            }

            // Participant name
            right.add(new Paragraph(
                    card.getParticipantName() != null ? card.getParticipantName() : "Guest")
                    .setFontSize(13).setBold()
                    .setFontColor(new DeviceRgb(255, 255, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(14));

            // Invitation ID — monospace style
            right.add(new Paragraph("ID: " + card.getInvitationId())
                    .setFontSize(7)
                    .setFontColor(c.lightText)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(4));

            // Layout label
            right.add(new Paragraph(event.getSelectedLayout() != null
                    ? event.getSelectedLayout().name() + " LAYOUT" : "")
                    .setFontSize(7).setItalic()
                    .setFontColor(c.lightText)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8));

            root.addCell(right);
            doc.add(root);

        } catch (Exception e) {
            log.error("Error generating PDF card: {}", e.getMessage(), e);
            throw new IOException("Failed to generate PDF card: " + e.getMessage());
        }

        log.info("PDF card generated: {}", pdfPath);
        return pdfPath.toString();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Paragraph detail(String icon, String label, String value, LayoutColors c) {
        Paragraph p = new Paragraph();
        p.add(new Text(icon + "  ").setFontSize(10));
        p.add(new Text(label + ": ").setFontSize(9.5f).setFontColor(c.labelText));
        p.add(new Text(value).setFontSize(9.5f).setFontColor(new DeviceRgb(255, 255, 255)));
        p.setMarginBottom(7);
        return p;
    }

    private LayoutColors getLayoutColors(EventLayout layout) {
        if (layout == null) return blue();
        return switch (layout) {
            case CLASSIC    -> new LayoutColors(new DeviceRgb(26, 35, 126),  new DeviceRgb(40, 53, 147),   new DeviceRgb(180, 190, 240), new DeviceRgb(140, 160, 220));
            case MODERN     -> new LayoutColors(new DeviceRgb(0, 77, 64),    new DeviceRgb(0, 105, 92),    new DeviceRgb(160, 210, 200), new DeviceRgb(120, 180, 170));
            case CORPORATE  -> new LayoutColors(new DeviceRgb(38, 50, 56),   new DeviceRgb(55, 71, 79),    new DeviceRgb(160, 180, 190), new DeviceRgb(130, 155, 165));
            case WEDDING    -> new LayoutColors(new DeviceRgb(136, 14, 79),  new DeviceRgb(173, 20, 87),   new DeviceRgb(230, 160, 200), new DeviceRgb(200, 130, 170));
            case CONFERENCE -> new LayoutColors(new DeviceRgb(191, 54, 12),  new DeviceRgb(216, 67, 21),   new DeviceRgb(245, 180, 150), new DeviceRgb(220, 150, 120));
            case PREMIUM    -> new LayoutColors(new DeviceRgb(74, 20, 140),  new DeviceRgb(106, 27, 154),  new DeviceRgb(200, 160, 240), new DeviceRgb(170, 130, 210));
        };
    }

    private LayoutColors blue() {
        return new LayoutColors(
                new DeviceRgb(26, 86, 219), new DeviceRgb(20, 65, 160),
                new DeviceRgb(200, 210, 240), new DeviceRgb(160, 180, 230));
    }

    private record LayoutColors(Color primary, Color secondary, Color lightText, Color labelText) {}
}
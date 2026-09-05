package com.powerfitness.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Payment;
import com.powerfitness.entity.Supplement;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private static final Color DARK_HEADER = new Color(16, 22, 34);
    private static final Color ORANGE_ACCENT = new Color(255, 87, 34);
    private static final Color LIGHT_ROW = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    private void addHeader(Document doc, String title, String subtitle) throws DocumentException {
        Paragraph brand = new Paragraph("POWER FITNESS UNISEX GYM KURNOOL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, ORANGE_ACCENT));
        brand.setAlignment(Element.ALIGN_CENTER);
        doc.add(brand);

        Paragraph motto = new Paragraph("\"BUILD YOUR BODY. BUILD YOUR DISCIPLINE.\"", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.DARK_GRAY));
        motto.setAlignment(Element.ALIGN_CENTER);
        doc.add(motto);

        Paragraph reportTitle = new Paragraph(title.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, DARK_HEADER));
        reportTitle.setAlignment(Element.ALIGN_CENTER);
        reportTitle.setSpacingBefore(8);
        doc.add(reportTitle);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        Paragraph datePar = new Paragraph(subtitle + " | Generated on: " + dateStr, FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
        datePar.setAlignment(Element.ALIGN_CENTER);
        datePar.setSpacingAfter(14);
        doc.add(datePar);
    }

    private void styleHeaderCell(PdfPCell cell) {
        cell.setBackgroundColor(DARK_HEADER);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    }

    private void styleDataCell(PdfPCell cell, boolean isAlternate) {
        cell.setBackgroundColor(isAlternate ? LIGHT_ROW : Color.WHITE);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    }

    public byte[] generateMembersPdf(List<Member> members) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 25, 25);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Official Gym Members Directory", "Total Registered Athletes: " + members.size());

        float[] colWidths = {1.2f, 2.2f, 1.6f, 1.4f, 1.4f, 1.6f, 1.0f, 1.4f, 1.4f, 1.4f, 1.2f, 1.4f};
        PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);

        String[] headers = {"Member ID", "Full Name", "Phone Number", "Admission", "Plan", "Category", "Cardio", "Batch", "Fee", "Expiry", "Days Left", "Status"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            styleHeaderCell(c);
            table.addCell(c);
        }

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean alt = false;
        for (Member m : members) {
            PdfPCell[] cells = {
                new PdfPCell(new Phrase(m.getMemberCode(), font)),
                new PdfPCell(new Phrase(m.getFullName(), font)),
                new PdfPCell(new Phrase(m.getPhoneNumber(), font)),
                new PdfPCell(new Phrase(String.valueOf(m.getAdmissionDate()), font)),
                new PdfPCell(new Phrase(m.getSubscriptionPlan(), font)),
                new PdfPCell(new Phrase(m.getTrainingCategory(), font)),
                new PdfPCell(new Phrase(m.isHasCardio() ? "Yes" : "No", font)),
                new PdfPCell(new Phrase(m.getBatch(), font)),
                new PdfPCell(new Phrase("Rs." + (int)m.getTotalFee(), font)),
                new PdfPCell(new Phrase(String.valueOf(m.getExpiryDate()), font)),
                new PdfPCell(new Phrase(String.valueOf(m.getDaysRemaining()), font)),
                new PdfPCell(new Phrase(m.getStatus(), font))
            };
            for (PdfPCell cell : cells) {
                styleDataCell(cell, alt);
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    public byte[] generatePaymentsPdf(List<Payment> payments, double totalRevenue) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 25, 25);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Official Payments & Revenue Audit", String.format("Total Recorded Revenue: Rs. %,.2f | Transactions: %d", totalRevenue, payments.size()));

        float[] colWidths = {2.0f, 1.4f, 2.4f, 1.6f, 1.4f, 1.4f, 1.6f, 1.4f, 1.4f, 1.4f};
        PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);

        String[] headers = {"Receipt No", "Member ID", "Member Name", "Plan", "Base Fee", "Cardio Fee", "Total Amount", "Payment Date", "Method", "Status"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            styleHeaderCell(c);
            table.addCell(c);
        }

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean alt = false;
        for (Payment p : payments) {
            String mName = p.getMember() != null ? p.getMember().getFullName() : "N/A";
            String mCode = p.getMember() != null ? p.getMember().getMemberCode() : "N/A";
            PdfPCell[] cells = {
                new PdfPCell(new Phrase(p.getReceiptNumber(), font)),
                new PdfPCell(new Phrase(mCode, font)),
                new PdfPCell(new Phrase(mName, font)),
                new PdfPCell(new Phrase(p.getSubscriptionPlan(), font)),
                new PdfPCell(new Phrase("Rs." + (int)p.getBaseFee(), font)),
                new PdfPCell(new Phrase("Rs." + (int)p.getCardioFee(), font)),
                new PdfPCell(new Phrase("Rs." + (int)p.getAmount(), font)),
                new PdfPCell(new Phrase(String.valueOf(p.getPaymentDate()), font)),
                new PdfPCell(new Phrase(p.getPaymentMethod(), font)),
                new PdfPCell(new Phrase(p.getPaymentStatus(), font))
            };
            for (PdfPCell cell : cells) {
                styleDataCell(cell, alt);
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    public byte[] generateSubscriptionsPdf(List<Member> members) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 25, 25, 25, 25);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Subscription Duration & Expiry Report", "Total Subscriptions Monitored: " + members.size());

        float[] colWidths = {1.5f, 2.5f, 1.8f, 1.8f, 1.8f, 1.4f, 1.4f, 1.8f};
        PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);

        String[] headers = {"Member ID", "Member Name", "Plan", "Start Date", "Expiry Date", "Duration", "Remaining", "Status"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            styleHeaderCell(c);
            table.addCell(c);
        }

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean alt = false;
        for (Member m : members) {
            PdfPCell[] cells = {
                new PdfPCell(new Phrase(m.getMemberCode(), font)),
                new PdfPCell(new Phrase(m.getFullName(), font)),
                new PdfPCell(new Phrase(m.getSubscriptionPlan(), font)),
                new PdfPCell(new Phrase(String.valueOf(m.getStartDate()), font)),
                new PdfPCell(new Phrase(String.valueOf(m.getExpiryDate()), font)),
                new PdfPCell(new Phrase(m.getTotalDays() + " days", font)),
                new PdfPCell(new Phrase(m.getDaysRemaining() + " days", font)),
                new PdfPCell(new Phrase(m.getStatus(), font))
            };
            for (PdfPCell cell : cells) {
                styleDataCell(cell, alt);
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    public byte[] generateSupplementsPdf(List<Supplement> supplements) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 25, 25, 25, 25);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Gym Supplements Inventory Report", "Available Store Catalog: " + supplements.size() + " Products");

        float[] colWidths = {1.2f, 3.0f, 1.8f, 1.6f, 1.8f, 4.0f};
        PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);

        String[] headers = {"Product ID", "Product Name", "Category", "Price", "Availability", "Description"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            styleHeaderCell(c);
            table.addCell(c);
        }

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean alt = false;
        for (Supplement s : supplements) {
            PdfPCell[] cells = {
                new PdfPCell(new Phrase("SUP-" + s.getId(), font)),
                new PdfPCell(new Phrase(s.getName(), font)),
                new PdfPCell(new Phrase(s.getCategory(), font)),
                new PdfPCell(new Phrase("Rs." + (int)s.getPrice(), font)),
                new PdfPCell(new Phrase("AVAILABLE".equals(s.getStockStatus()) ? "Available" : "Out of Stock", font)),
                new PdfPCell(new Phrase(s.getDescription() != null ? s.getDescription() : "", font))
            };
            for (PdfPCell cell : cells) {
                styleDataCell(cell, alt);
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    public byte[] generateRevenuePdf(double totalRev, double subRev, double cardioRev, List<Payment> payments) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 25, 25, 25, 25);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Executive Revenue Summary", "Power Fitness Unisex GYM Kurnool Comprehensive Fiscal Report");

        // Summary Boxes
        PdfPTable sumTable = new PdfPTable(3);
        sumTable.setWidthPercentage(100);
        sumTable.setSpacingAfter(15);

        Font sumTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
        Font sumValFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, DARK_HEADER);

        PdfPCell c1 = new PdfPCell();
        c1.setPadding(8);
        c1.setBorderColor(BORDER_COLOR);
        c1.addElement(new Phrase("Total Gross Revenue", sumTitleFont));
        c1.addElement(new Phrase(String.format("Rs. %,.2f", totalRev), sumValFont));
        sumTable.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setPadding(8);
        c2.setBorderColor(BORDER_COLOR);
        c2.addElement(new Phrase("Base Subscription Share", sumTitleFont));
        c2.addElement(new Phrase(String.format("Rs. %,.2f", subRev), sumValFont));
        sumTable.addCell(c2);

        PdfPCell c3 = new PdfPCell();
        c3.setPadding(8);
        c3.setBorderColor(BORDER_COLOR);
        c3.addElement(new Phrase("Cardio Add-On Share", sumTitleFont));
        c3.addElement(new Phrase(String.format("Rs. %,.2f", cardioRev), sumValFont));
        sumTable.addCell(c3);

        doc.add(sumTable);

        // Recent Payments
        Paragraph pTitle = new Paragraph("TRANSACTION AUDIT LOG", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_HEADER));
        pTitle.setSpacingAfter(8);
        doc.add(pTitle);

        float[] colWidths = {2.2f, 2.5f, 1.8f, 1.8f, 1.8f, 1.8f};
        PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);

        String[] headers = {"Receipt No", "Athlete Name", "Plan", "Amount", "Date", "Status"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            styleHeaderCell(c);
            table.addCell(c);
        }

        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean alt = false;
        for (Payment p : payments) {
            String mName = p.getMember() != null ? p.getMember().getFullName() : "N/A";
            PdfPCell[] cells = {
                new PdfPCell(new Phrase(p.getReceiptNumber(), font)),
                new PdfPCell(new Phrase(mName, font)),
                new PdfPCell(new Phrase(p.getSubscriptionPlan(), font)),
                new PdfPCell(new Phrase(String.format("Rs. %,.2f", p.getAmount()), font)),
                new PdfPCell(new Phrase(String.valueOf(p.getPaymentDate()), font)),
                new PdfPCell(new Phrase(p.getPaymentStatus(), font))
            };
            for (PdfPCell cell : cells) {
                styleDataCell(cell, alt);
                table.addCell(cell);
            }
            alt = !alt;
        }

        doc.add(table);
        doc.close();
        return out.toByteArray();
    }
}

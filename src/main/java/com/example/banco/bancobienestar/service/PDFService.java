package com.example.banco.bancobienestar.service;

import com.example.banco.bancobienestar.model.SolicitudCreditoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import com.example.banco.bancobienestar.model.MovimientoEntity;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PDFService {

    public ByteArrayInputStream generarEstadoCuenta(UsuarioEntity cliente, List<MovimientoEntity> movimientos) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            document.add(new Paragraph("ESTADO DE CUENTA").setFont(fontBold).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cliente: " + cliente.getNombre()).setFont(font));
            document.add(new Paragraph("Usuario: " + cliente.getUsername()).setFont(font));
            document.add(new Paragraph(" "));

            if (movimientos != null && !movimientos.isEmpty()) {
                Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 2}));
                table.addCell(new Cell().add(new Paragraph("Fecha").setFont(fontBold)));
                table.addCell(new Cell().add(new Paragraph("Descripción").setFont(fontBold)));
                table.addCell(new Cell().add(new Paragraph("Monto").setFont(fontBold)));
                table.addCell(new Cell().add(new Paragraph("Tipo").setFont(fontBold)));

                for (MovimientoEntity mov : movimientos) {
                    table.addCell(new Paragraph(mov.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).setFont(font));
                    table.addCell(new Paragraph(mov.getDescripcion() != null ? mov.getDescripcion() : "-").setFont(font));
                    
                    Cell montoCell = new Cell().add(new Paragraph("$" + String.format("%.2f", mov.getMonto())).setFont(font));
                    if (mov.getMonto() >= 0) {
                        montoCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN);
                    } else {
                        montoCell.setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED);
                    }
                    table.addCell(montoCell);
                    
                    table.addCell(new Paragraph(mov.getTipo() != null ? mov.getTipo() : "-").setFont(font));
                }
                document.add(table);
            } else {
                document.add(new Paragraph("No hay movimientos registrados").setFont(font));
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generarContratoCredito(SolicitudCreditoEntity solicitud) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            document.add(new Paragraph("CONTRATO DE CRÉDITO").setFont(fontBold).setFontSize(24).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("N° " + solicitud.getId()).setFont(font).setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cliente: " + solicitud.getUsuario().getNombre()).setFont(font));
            document.add(new Paragraph("Monto Solicitado: $" + String.format("%.2f", solicitud.getMontoSolicitado())).setFont(fontBold));
            document.add(new Paragraph("Fecha: " + solicitud.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(font));
            document.add(new Paragraph("Estado: " + solicitud.getEstado()).setFont(font));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Firma del Cliente: ___________________").setFont(font));
            document.add(new Paragraph("Firma del Banco: ___________________").setFont(font));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
}
package com.example.eventsphere.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeService {

    @Value("${app.cards.dir:./generated-cards}")
    private String cardsDir;

    public String generateQrCode(String content, String filename) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);        // tight quiet zone — white padding added in PDF
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        // 500×500 — high-res, sharp when printed
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 500, 500, hints);

        Path qrDir = Paths.get(cardsDir, "qrcodes");
        Files.createDirectories(qrDir);
        Path qrPath = qrDir.resolve(filename + ".png");

        // Black QR on white background
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
        MatrixToImageWriter.writeToPath(matrix, "PNG", qrPath, config);

        log.info("QR code generated: {}", qrPath);
        return qrPath.toString();
    }
}
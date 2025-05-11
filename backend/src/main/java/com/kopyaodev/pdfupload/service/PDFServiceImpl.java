package com.kopyaodev.pdfupload.service;

import com.kopyaodev.pdfupload.exception.PdfProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import jakarta.annotation.PostConstruct;

@Service
public class PDFServiceImpl implements PDFService {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFServiceImpl.class);
    
    @Value("${pdf.upload.path}")
    private String uploadPath;
    
    @Value("${pdf.max.size:10485760}") // varsayılan 10MB
    private long maxFileSize;

    @PostConstruct
    public void init() {
        // Upload dizinini oluştur
        try {
            Files.createDirectories(Paths.get(uploadPath));
            logger.info("Upload dizini oluşturuldu: {}", uploadPath);
        } catch (IOException e) {
            logger.error("Upload dizini oluşturulamadı: {}", e.getMessage());
        }
    }

    @Override
    public void processPdf(byte[] pdfData, String username) throws PdfProcessingException {
        try {
            // Benzersiz dosya adı oluştur
            String fileName = username + "_" + System.currentTimeMillis() + ".pdf";
            Path filePath = Paths.get(uploadPath, fileName);
            
            // Dosyayı kaydet
            Files.write(filePath, pdfData);
            logger.info("PDF kaydedildi: {}", filePath);
            
        } catch (IOException e) {
            logger.error("PDF kaydedilemedi: {}", e.getMessage());
            throw new PdfProcessingException("PDF kaydedilemedi: " + e.getMessage());
        }
    }

    private void validateInput(byte[] pdfData, String username) {
        if (pdfData == null || pdfData.length == 0) {
            throw new PdfProcessingException("PDF verisi boş olamaz");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new PdfProcessingException("Kullanıcı adı boş olamaz");
        }
        if (pdfData.length > maxFileSize) {
            throw new PdfProcessingException(
                String.format("PDF boyutu çok büyük. Maximum: %d bytes, Gönderilen: %d bytes", 
                maxFileSize, pdfData.length)
            );
        }
    }

    private String generateFileName(String username) {
        return username + "_" + UUID.randomUUID().toString() + ".pdf";
    }

    private Path createUploadPath(String fileName) {
        try {
            Path directory = Paths.get(uploadPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            return directory.resolve(fileName);
        } catch (IOException e) {
            throw new PdfProcessingException("Upload dizini oluşturulamadı", e);
        }
    }

    private void validatePdfFormat(byte[] pdfData) {
        // PDF magic number kontrolü: %PDF-
        byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D};
        for (int i = 0; i < pdfHeader.length; i++) {
            if (pdfData[i] != pdfHeader[i]) {
                throw new PdfProcessingException("Geçersiz PDF formatı");
            }
        }
    }

    private void writeFile(byte[] pdfData, Path filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(pdfData);
            fos.flush();
        } catch (IOException e) {
            throw new PdfProcessingException("PDF dosyasi yazilamadi", e);
        }
    }
}

package com.kopyaodev.pdfupload.controller;


import com.kopyaodev.pdfupload.model.SignedPdfRequest; // bizim classlarimiz
import com.kopyaodev.pdfupload.service.PDFService;     // bizim service classimiz
import com.kopyaodev.pdfupload.utils.SignatureUtils;  // bizim utils classimiz
import org.springframework.http.HttpStatus;           // HTTP status kodlarini tutan class
import org.springframework.http.ResponseEntity;       // HTTP yanitlarini tutan class
import org.springframework.web.bind.annotation.*;         // HTTP isteklerini yonetmek icin kullanilan annotation'lar
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf") // endpoint soyle gorunecek Url'de: http://localhost:8080/api/pdf
@CrossOrigin(origins = "*")
public class PDFController {

    private final PDFService pdfService;
    private final SignatureUtils signatureUtils;

    public PDFController(PDFService pdfService, SignatureUtils signatureUtils) {
        this.pdfService = pdfService;
        this.signatureUtils = signatureUtils;
    }

@PostMapping("/upload")
public ResponseEntity<String> uploadPdf(
        @RequestParam("file") MultipartFile file,
        @RequestParam("username")  String username,
        @RequestParam("timestamp") String timestamp,
        @RequestParam("signature") String signature) {

    try {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body("Dosya boş");

        // 5 dk limit
        if (!isTimestampValid(Long.parseLong(timestamp)))
            return ResponseEntity.status(400).body("Timestamp eski");

        // HMAC kontrolü
        String message = username + timestamp;

        pdfService.processPdf(file.getBytes(), username);
        return ResponseEntity.ok("PDF başarıyla yüklendi");

    } catch (Exception e) {
        return ResponseEntity.status(500).body("Hata: " + e.getMessage());
    }
}

    private boolean validateSignature(SignedPdfRequest request) {
        String message = request.getUsername() + request.getTimestamp();
        return signatureUtils.verifyHmacSignature(message, request.getSignature());
    }

    private boolean isTimestampValid(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long requestAge = currentTime - timestamp;
        // 5 dakika (300,000 milisaniye) içinde gelen istekleri kabul et
        return requestAge <= 300_000;
    }
}





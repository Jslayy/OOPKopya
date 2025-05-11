package com.kopyaodev.pdfupload.service;

public interface PDFService { // PdfServiceImpl icin interface
    void processPdf(byte[] pdfData, String username);
}
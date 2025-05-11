package com.kopyaodev.pdfupload.model;

public class SignedPdfRequest {


    private String username;   // Kullanıcının adı
    private String signature;  // HMAC veya başka bir imza (security icin)
    private long timestamp;    // Zaman damgası (replay attack onlemek icin)
    private byte[] pdfData;    // PDF'in binary verisi

    // Bos constructor (Spring Boot JSON'u deserialize edebilmesi icin)
    public SignedPdfRequest() {}

    public SignedPdfRequest(String username, String signature, long timestamp, byte[] pdfData) {
        this.username = username;
        this.signature = signature;
        this.timestamp = timestamp;
        this.pdfData = pdfData;
    }

    // Getter ve Setter Metotlari
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public byte[] getPdfData() { return pdfData; }
    public void setPdfData(byte[] pdfData) { this.pdfData = pdfData; }
}

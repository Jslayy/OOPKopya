package com.kopyaodev.pdfupload.utils;

import com.kopyaodev.pdfupload.exception.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SignatureUtils {
    
    @Value("${jwt.secret.key}")
    private String secretKey;

    public String createHmacSignature(String message) {
        if (message == null || message.isEmpty()) {
            throw new SignatureException("İmzalanacak mesaj boş olamaz");
        }

        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            sha256Hmac.init(secretKeySpec);
            
            byte[] hash = sha256Hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new SignatureException("HmacSHA256 algoritmasi bulunamadi", e);
        } catch (java.security.InvalidKeyException e) {
            throw new SignatureException("Geçersiz secret key", e);
        } catch (Exception e) {
            throw new SignatureException("İmza oluşturma sirasinda beklenmeyen hata", e);
        }
    }

    public boolean verifyHmacSignature(String message, String signature) {
        if (message == null || signature == null) {
            throw new SignatureException("Mesaj veya imza boş olamaz");
        }

        try {
            String calculatedSignature = createHmacSignature(message);
            return calculatedSignature.equals(signature);
        } catch (SignatureException e) {
            throw new SignatureException("İmza doğrulama sirasinda hata: " + e.getMessage(), e);
        }
    }
}


package com.kopyaodev.pdfupload.model;

public class LoginRequest {
    

    private String username;  // Kullanıcı adı
    private String password;  // Kullanıcı şifresi

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter ve Setter Metotları
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
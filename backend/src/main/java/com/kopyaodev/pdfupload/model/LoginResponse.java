package com.kopyaodev.pdfupload.model;

import java.util.List;

public class LoginResponse {

    private String token;
    private List<String> roles;

    /** JSON-deserialization için boş yapıcı (gerekirse) */
    public LoginResponse() {}

    /** Eski tek-parametreli sürüm (sadece token) */
    public LoginResponse(String token) {
        this.token = token;
    }

    /** Yeni sürüm: token + roller */
    public LoginResponse(String token, List<String> roles) {
        this.token = token;
        this.roles = roles;
    }

    /* ---------- getters & setters ---------- */

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}

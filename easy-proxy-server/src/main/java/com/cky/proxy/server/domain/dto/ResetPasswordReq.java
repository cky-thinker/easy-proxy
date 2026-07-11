package com.cky.proxy.server.domain.dto;

public class ResetPasswordReq {
    private Integer id;
    private String encryptedPassword;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }
}

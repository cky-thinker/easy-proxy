package com.cky.proxy.server.domain.dto;

public class LoginConfigResp {
    private Boolean captchaImageEnable;
    private Boolean passwordEncryptEnable;
    private String passwordPublicKey;

    public Boolean getCaptchaImageEnable() { return captchaImageEnable; }
    public void setCaptchaImageEnable(Boolean captchaImageEnable) { this.captchaImageEnable = captchaImageEnable; }

    public Boolean getPasswordEncryptEnable() { return passwordEncryptEnable; }
    public void setPasswordEncryptEnable(Boolean passwordEncryptEnable) { this.passwordEncryptEnable = passwordEncryptEnable; }

    public String getPasswordPublicKey() { return passwordPublicKey; }
    public void setPasswordPublicKey(String passwordPublicKey) { this.passwordPublicKey = passwordPublicKey; }
}

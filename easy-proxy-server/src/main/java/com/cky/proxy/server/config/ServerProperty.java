package com.cky.proxy.server.config;

public class ServerProperty {
    private int proxyPort;  
    private int webPort;
    private boolean captchaImageEnable;

    public int getProxyPort() { return proxyPort; }
    public void setProxyPort(int proxyPort) { this.proxyPort = proxyPort; }

    public int getWebPort() { return webPort; }
    public void setWebPort(int webPort) { this.webPort = webPort; }

    public boolean getCaptchaImageEnable() { return captchaImageEnable; }
    public void setCaptchaImageEnable(boolean captchaImageEnable) { this.captchaImageEnable = captchaImageEnable; }
}

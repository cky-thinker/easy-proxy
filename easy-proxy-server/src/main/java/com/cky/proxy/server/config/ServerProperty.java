package com.cky.proxy.server.config;

public class ServerProperty {
    private int proxyPort;  
    private int webPort;
    private boolean catureImageEnable;

    public int getProxyPort() { return proxyPort; }
    public void setProxyPort(int proxyPort) { this.proxyPort = proxyPort; }

    public int getWebPort() { return webPort; }
    public void setWebPort(int webPort) { this.webPort = webPort; }

    public boolean getCatureImageEnable() { return catureImageEnable; }
    public void setCatureImageEnable(boolean catureImage) { this.catureImageEnable = catureImage; }
}

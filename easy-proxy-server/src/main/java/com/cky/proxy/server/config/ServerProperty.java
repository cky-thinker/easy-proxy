package com.cky.proxy.server.config;

import lombok.Data;

@Data
public class ServerProperty {
    private int proxyPort;  
    private int webPort;
    private Boolean captchaImageEnable;
    private String publicHost;
    private int certValidityDays;
    private String certPassword;
}

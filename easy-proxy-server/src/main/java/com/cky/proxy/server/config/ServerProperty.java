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
    private String webhook;
    private Integer loginFailureThreshold = 5;
    private Integer loginFailureWindowMinutes = 15;
    private Integer loginLockMinutes = 15;
    private Integer loginIpFailureThreshold = 20;
    private Integer loginIpWindowMinutes = 10;
    private Integer loginIpBlockMinutes = 10;
}

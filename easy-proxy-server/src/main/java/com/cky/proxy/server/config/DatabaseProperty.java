package com.cky.proxy.server.config;

import lombok.Data;

@Data
public class DatabaseProperty {
    private String driver;
    private String url;
    private String username;
    private String password;
    private boolean h2ConsoleEnable;
}

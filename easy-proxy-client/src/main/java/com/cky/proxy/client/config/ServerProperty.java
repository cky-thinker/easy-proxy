package com.cky.proxy.client.config;

import lombok.Data;

@Data
public class ServerProperty {
    private String ip;
    private int port;
    private String token;
}

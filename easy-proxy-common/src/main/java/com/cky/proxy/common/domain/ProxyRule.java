package com.cky.proxy.common.domain;

import lombok.Data;

@Data
public class ProxyRule {
    private String name;
    private int serverPort;
    private String clientAddress;
}

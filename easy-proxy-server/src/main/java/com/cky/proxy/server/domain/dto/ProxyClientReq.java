package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class ProxyClientReq {
    private String q;
    private String status;
    private Boolean enableFlag;
}
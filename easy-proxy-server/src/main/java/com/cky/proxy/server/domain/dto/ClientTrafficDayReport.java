package com.cky.proxy.server.domain.dto;

import java.util.Date;

import lombok.Data;

@Data
public class ClientTrafficDayReport {
    private Integer proxyClientId;
    private Date date;
    private Long uploadBytes;
    private Long downloadBytes;
}
package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class SysLogReq {
    private String logType;
    private String keyword;
}
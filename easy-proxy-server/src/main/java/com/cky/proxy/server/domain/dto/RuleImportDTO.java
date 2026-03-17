package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class RuleImportDTO {
    /**
     * 规则监听端口
     */
    private Integer inetPort;
    /**
     * 目标地址
     */
    private String lan;
    /**
     * 名称
     */
    private String name;
}

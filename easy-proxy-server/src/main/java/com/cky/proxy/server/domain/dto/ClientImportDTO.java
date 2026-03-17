package com.cky.proxy.server.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class ClientImportDTO {
    /**
     * 客户端名称
     */
    private String name;
    /**
     * token
     */
    private String clientKey;
    /**
     * 状态 (将被忽略，使用全局 enableFlag)
     */
    private Integer status;
    /**
     * 规则列表
     */
    private List<RuleImportDTO> proxyMappings;
}

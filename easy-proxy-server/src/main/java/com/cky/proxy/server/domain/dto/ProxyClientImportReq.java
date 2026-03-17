package com.cky.proxy.server.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class ProxyClientImportReq {
    /**
     * 默认是否启用
     */
    private Boolean enableFlag;
    /**
     * 客户端列表
     */
    private List<ClientImportDTO> clients;
}

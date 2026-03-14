package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class TrafficRankingDTO {
    private Integer proxyClientRuleId;
    private Long totalTraffic;
}

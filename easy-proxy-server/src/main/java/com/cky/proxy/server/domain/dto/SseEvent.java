package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class SseEvent {
    private String eventType;
    private Object data;
}

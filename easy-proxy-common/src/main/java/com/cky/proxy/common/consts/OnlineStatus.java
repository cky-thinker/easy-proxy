package com.cky.proxy.common.consts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OnlineStatus {
    online("在线"),
    offline( "离线");

    private final String desc;
}

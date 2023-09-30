package com.cky.proxy.server.domain;

import com.cky.proxy.common.domain.ProxyClientConfig;
import com.cky.proxy.server.consts.ModifyType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataModifyEvent {
    private ModifyType type;
    private ProxyClientConfig newConfig;

    public static DataModifyEvent add(ProxyClientConfig newConfig) {
        return new DataModifyEvent().setType(ModifyType.UPDATE).setNewConfig(newConfig);
    }

    public static DataModifyEvent delete(ProxyClientConfig newConfig) {
        return new DataModifyEvent().setType(ModifyType.DELETE).setNewConfig(newConfig);
    }

    public static DataModifyEvent update(ProxyClientConfig newConfig) {
        return new DataModifyEvent().setType(ModifyType.UPDATE).setNewConfig(newConfig);
    }
}

package com.cky.proxy.client.domain;

import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ReUtil;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Address {
    private String ip;
    private int port;
    private boolean valid;

    public static Address parse(String address) {
        String[] addrs = address.split(":");
        if (addrs.length != 2) {
            return new Address().setValid(false);
        }
        if (!"localhost".equals(addrs[0]) && !ReUtil.isMatch(RegexPool.IPV4, addrs[0])) {
            return new Address().setValid(false);
        }
        if (!ReUtil.isMatch(RegexPool.NUMBERS, addrs[1])) {
            return new Address().setValid(false);
        }
        return new Address().setIp(addrs[0]).setPort(Integer.parseInt(addrs[1])).setValid(true);
    }
}

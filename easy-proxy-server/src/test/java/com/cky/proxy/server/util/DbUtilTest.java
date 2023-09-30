package com.cky.proxy.server.util;

import com.cky.proxy.common.domain.ProxyClientConfig;
import com.cky.proxy.common.domain.ProxyRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

@Slf4j
class DbUtilTest {
    @Test
    public void flushProxyClients() {
        ProxyRule rule = new ProxyRule();
        rule.setClientAddress("127.0.0.1:8888");
        rule.setServerPort(18888);
        rule.setName("8888proxy");
        ProxyClientConfig proxyClientConfig = new ProxyClientConfig();
        proxyClientConfig.setName("test");
        proxyClientConfig.setToken("1234567");
        proxyClientConfig.setProxyRules(Arrays.asList(rule));
        DbUtil.getProxyClients();
        DbUtil.updateProxyClient(proxyClientConfig);
        Collection<ProxyClientConfig> proxyClientConfigs = DbUtil.getProxyClients();
        log.info("{}", proxyClientConfigs);
    }
}

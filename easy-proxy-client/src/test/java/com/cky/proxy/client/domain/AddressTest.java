package com.cky.proxy.client.domain;

import org.junit.jupiter.api.Test;

class AddressTest {
    @Test
    public void parse() {
        String addr = "127.0.0.1:8888";
        Address parse = Address.parse(addr);
        System.out.println(parse);
    }

}

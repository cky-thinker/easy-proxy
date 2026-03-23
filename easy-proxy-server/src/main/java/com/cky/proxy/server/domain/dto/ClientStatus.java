package com.cky.proxy.server.domain.dto;

import com.cky.proxy.server.util.JsonUtil;

public class ClientStatus {
    private String key;
    private boolean online;

    public ClientStatus(String key, boolean online) {
        this.key = key;
        this.online = online;
    }

    public ClientStatus() {
    }

    public static ClientStatus online(String key) {
        ClientStatus result = new ClientStatus();
        result.key = key;
        result.online = true;
        return result;
    }

    public static ClientStatus offline(String key) {
        ClientStatus result = new ClientStatus();
        result.key = key;
        result.online = false;
        return result;
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    public String getKey() {
        return key;
    }

    public boolean isOnline() {
        return online;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}

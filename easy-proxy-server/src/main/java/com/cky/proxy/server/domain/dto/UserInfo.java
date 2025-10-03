package com.cky.proxy.server.domain.dto;

public class UserInfo {
    private Integer userId;
    private String username;
    private String avatar;
    private String token;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}

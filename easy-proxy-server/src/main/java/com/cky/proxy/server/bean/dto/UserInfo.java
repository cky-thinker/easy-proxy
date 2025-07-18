package com.cky.proxy.server.bean.dto;

import lombok.Data;

@Data
public class UserInfo {
    private Integer userId;
    private String username;
    private String avatar;
    private String token;
}

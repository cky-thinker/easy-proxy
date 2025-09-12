package com.cky.proxy.server.domain.dto;

import lombok.Data;

@Data
public class LoginReq {
    private String username;
    private String password;
    private String captchaId;
    private String captchaCode;
}

package com.cky.proxy.server.bean.dto;

import lombok.Data;

@Data
public class LoginReq {
    private String username;
    private String password;
    private String captchaId;
    private String captchaCode;
}

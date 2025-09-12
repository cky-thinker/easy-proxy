package com.cky.proxy.server.domain.dto;

public class Result<T> {
    public int code;
    public String msg;
    public T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = "";
        result.data = data;
        return result;
    }

    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = msg;
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(String errorMsg) {
        Result<T> result = new Result<>();
        result.code = 500;
        result.msg = errorMsg;
        return result;
    }
}

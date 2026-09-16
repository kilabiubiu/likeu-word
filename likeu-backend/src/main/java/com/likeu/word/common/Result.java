package com.likeu.word.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一Result返回体
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp = System.currentTimeMillis();

    public static <T> Result<T> success() {
        return build(ResultCode.SUCCESS, null);
    }

    public static <T> Result<T> success(T data) {
        return build(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> success(String msg, T data) {
        Result<T> r = build(ResultCode.SUCCESS, data);
        r.setMessage(msg);
        return r;
    }

    public static <T> Result<T> fail() {
        return build(ResultCode.FAIL, null);
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> r = build(ResultCode.FAIL, null);
        r.setMessage(msg);
        return r;
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return build(rc, null);
    }

    public static <T> Result<T> fail(ResultCode rc, String msg) {
        Result<T> r = build(rc, null);
        r.setMessage(msg);
        return r;
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(msg);
        return r;
    }

    private static <T> Result<T> build(ResultCode rc, T data) {
        Result<T> r = new Result<>();
        r.setCode(rc.getCode());
        r.setMessage(rc.getMessage());
        r.setData(data);
        return r;
    }
}
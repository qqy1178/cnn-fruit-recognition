package com.fruitrec.common;

import lombok.Data;

/**
 * 统一返回体。前端永远拿到 {code, msg, data} 这个形状，
 * 不用每个接口各写各的格式。
 */
@Data
public class Result<T> {

    /** 200 成功；其他为业务错误码 */
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }
}

package com.ytrue.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-08-28 10:59
 * @description 返回信息的实体类，调度中心和执行器都会用到
 */
@Data
public class ReturnT<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;

    public static final ReturnT<String> SUCCESS = new ReturnT<>(null);
    public static final ReturnT<String> FAIL = new ReturnT<>(FAIL_CODE, null);


    private int code;
    private String msg;
    private T content;

    public ReturnT() {
    }

    public ReturnT(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ReturnT(T content) {
        this.code = SUCCESS_CODE;
        this.content = content;
    }

}

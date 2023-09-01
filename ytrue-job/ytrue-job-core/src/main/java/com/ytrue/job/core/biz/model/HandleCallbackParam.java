package com.ytrue.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-09-01 9:36
 * @description 这个就是用于执行器回调定时任务执行结果的包装类执行结果的信息用这个类的对象封装
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandleCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    // 日志id
    private long logId;
    // 定时任务的触发时间，这个触发时间就是jobLog刚才设置的那个时间
    private long logDateTim;
    // 执行的响应码
    private int handleCode;
    //执行的具体结果
    private String handleMsg;


}

package com.ytrue.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-09-01 9:15
 * @description LogResult
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class LogResult implements Serializable {

    private static final long serialVersionUID = 42L;

    // 第几行开始
    private int fromLineNum;
    // 用于记录读取到的行数
    private int toLineNum;
    // 读取的内容
    private String logContent;
    // 是否读取结束
    private boolean isEnd;
}

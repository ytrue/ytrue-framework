package com.ytrue.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-09-01 9:15
 * @description LogParam
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class LogParam implements Serializable {

    private static final long serialVersionUID = 42L;

    // 日志时间
    private long logDateTim;
    // 日志id
    private long logId;
    // 多少行开始读
    private int fromLineNum;

}

package com.ytrue.job.core.context;

import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-28 11:21
 * @description 定时任务上下文
 */
@Getter
public class XxlJobContext {

    /**
     * 成功
     */
    public static final int HANDLE_CODE_SUCCESS = 200;
    /**
     * 失败
     */
    public static final int HANDLE_CODE_FAIL = 500;
    /**
     * 超时
     */
    public static final int HANDLE_CODE_TIMEOUT = 502;

    /**
     * 任务id
     */
    private final long jobId;


    /**
     * 任务参数
     */
    private final String jobParam;


    /**
     * 任务日志文件名
     */
    private final String jobLogFileName;


    /**
     * INDEX
     */
    private final int shardIndex;


    /**
     * 总数
     */
    private final int shardTotal;


    /**
     * 处理状态
     */
    private int handleCode;


    /**
     * 处理信息
     */
    private String handleMsg;

    public XxlJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        this.handleCode = HANDLE_CODE_SUCCESS;
    }


    /**
     * InheritableThreadLocal声明的变量同样是线程私有的，但是子线程可以从父线程继承InheritableThreadLocal声明的变量
     * 绑定线程
     */
    private static InheritableThreadLocal<XxlJobContext> contextHolder = new InheritableThreadLocal<>();


    public static void setXxlJobContext(XxlJobContext xxlJobContext) {
        contextHolder.set(xxlJobContext);
    }


    public static XxlJobContext getXxlJobContext() {
        return contextHolder.get();
    }
}

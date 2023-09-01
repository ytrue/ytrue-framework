package com.ytrue.job.core.context;

import lombok.Getter;
import lombok.Setter;

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
    @Setter
    private int handleCode;


    /**
     * 处理信息
     */
    @Setter
    private String handleMsg;

    public XxlJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        //构造方法中唯一值得注意的就是这里，创建XxlJobContext对象的时候默认定时任务的执行结果就是成功
        //如果执行失败了，自由其他方法把这里设置成失败
        this.handleCode = HANDLE_CODE_SUCCESS;
    }


    /**
     * 这里是一个线程的本地变量，因为定时任务真正执行的时候，在执行器端是一个定时任务任务对应一个线程
     * 这样就把定时任务隔离开了，自然就可以利用这个线程的本地变量，把需要的数据存储在里面
     * 这里使用的这个变量是可继承的threadlocal，也就子线程可以访问父线程存储在本地的数据了
     * <p>
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

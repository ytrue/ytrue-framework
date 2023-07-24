package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-24 9:24
 * @description 执行器选择工厂接口
 */
public interface EventExecutorChooserFactory {


    /**
     * 创建执行器
     *
     * @param executors
     * @return
     */
    EventExecutorChooser newChooser(EventExecutor[] executors);


    /**
     * 内部类
     */
    interface EventExecutorChooser {

        /**
         * 获取EventExecutor
         * Returns the new {@link EventExecutor} to use.
         */
        EventExecutor next();
    }
}

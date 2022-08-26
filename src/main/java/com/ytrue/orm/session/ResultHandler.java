package com.ytrue.orm.session;

/**
 * @author ytrue
 * @date 2022/8/23 15:04
 * @description 结果处理器
 */
public interface ResultHandler {
    /**
     * 处理结果
     *
     * @param context
     */
    void handleResult(ResultContext context);
}

package com.ytrue.orm.session;

/**
 * @author ytrue
 * @date 2022/8/25 15:43
 * @description 结果上下文
 */
public interface ResultContext {

    /**
     * 获取结果
     *
     * @return
     */
    Object getResultObject();

    /**
     * 获取记录数
     *
     * @return
     */
    int getResultCount();
}

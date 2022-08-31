package com.ytrue.orm.executor.result;

import com.ytrue.orm.session.ResultContext;

/**
 * @author ytrue
 * @date 2022/8/25 16:02
 * @description 默认结果上下文
 */
public class DefaultResultContext implements ResultContext {
    /**
     * 结果对象
     */
    private Object resultObject;


    /**
     * 结果个数
     */
    private int resultCount;

    public DefaultResultContext() {
        this.resultObject = null;
        this.resultCount = 0;
    }

    @Override
    public Object getResultObject() {
        return resultObject;
    }

    @Override
    public int getResultCount() {
        return resultCount;
    }

    public void nextResultObject(Object resultObject) {
        resultCount++;
        this.resultObject = resultObject;
    }
}

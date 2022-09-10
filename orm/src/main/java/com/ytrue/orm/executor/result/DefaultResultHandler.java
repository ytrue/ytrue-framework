package com.ytrue.orm.executor.result;

import com.ytrue.orm.reflection.factory.ObjectFactory;
import com.ytrue.orm.session.ResultContext;
import com.ytrue.orm.session.ResultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/25 16:02
 * @description DefaultResultHandler
 */
public class DefaultResultHandler implements ResultHandler {

    private final List<Object> list;

    public DefaultResultHandler() {
        this.list = new ArrayList<>();
    }

    /**
     * 通过 ObjectFactory 反射工具类，产生特定的 List
     */
    @SuppressWarnings("unchecked")
    public DefaultResultHandler(ObjectFactory objectFactory) {
        this.list = objectFactory.create(List.class);
    }

    @Override
    public void handleResult(ResultContext context) {
        list.add(context.getResultObject());
    }

    public List<Object> getResultList() {
        return list;
    }
}

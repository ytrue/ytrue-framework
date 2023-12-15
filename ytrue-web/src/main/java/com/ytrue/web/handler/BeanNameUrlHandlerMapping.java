package com.ytrue.web.handler;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ytrue
 * @date 2023-12-15 11:19
 * @description 不提供实现，只说明有这个场景
 */
public class BeanNameUrlHandlerMapping extends AbstractHandlerMapping{
    @Override
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) {
        return null;
    }

    @Override
    protected void detectHandlerMethod(String name) throws Exception {

    }

    @Override
    protected boolean isHandler(Class type) {
        return false;
    }

    @Override
    protected void setOrder(int order) {
        this.order = 2;
    }
}

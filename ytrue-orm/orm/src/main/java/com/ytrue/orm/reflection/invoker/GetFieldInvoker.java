package com.ytrue.orm.reflection.invoker;

import java.lang.reflect.Field;

/**
 * @author ytrue
 * @date 2022/8/19 08:54
 * @description getter 调用者
 */
public class GetFieldInvoker implements Invoker {
    private Field field;

    public GetFieldInvoker(Field field) {
        this.field = field;
    }


    @Override
    public Object invoke(Object target, Object[] args) throws Exception {
        return field.get(target);
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }
}

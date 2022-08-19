package com.ytrue.orm.reflection.invoker;

import java.lang.reflect.Field;

/**
 * @author ytrue
 * @date 2022/8/19 08:54
 * @description setter 调用者
 */
public class SetFieldInvoker implements Invoker {

    private Field field;

    public SetFieldInvoker(Field field) {
        this.field = field;
    }

    @Override
    public Object invoke(Object target, Object[] args) throws Exception {
        field.set(target, args);

        return null;
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }
}

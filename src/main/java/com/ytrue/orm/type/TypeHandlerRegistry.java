package com.ytrue.orm.type;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/23 16:28
 * @description 类型处理器注册机
 */
public class TypeHandlerRegistry {

    /**
     * JDBC 类型处理map
     */
    private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<>(JdbcType.class);

    private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new HashMap<>();

    private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<>();

    public TypeHandlerRegistry() {

    }

    private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
        if (null != javaType){
            Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.computeIfAbsent(javaType, k -> new HashMap<>());
            map.put(jdbcType, handler);
        }
        ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
    }

}

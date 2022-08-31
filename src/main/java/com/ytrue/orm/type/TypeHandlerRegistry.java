package com.ytrue.orm.type;

import com.ytrue.orm.mapping.DateTypeHandler;

import java.lang.reflect.Type;
import java.util.Date;
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

    /**
     * 类型处理器map
     */
    private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new HashMap<>();

    private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<>();

    public TypeHandlerRegistry() {

        // 注册long类型
        register(Long.class, new LongTypeHandler());
        register(long.class, new LongTypeHandler());

        // 注册string类型的
        register(String.class, new StringTypeHandler());
        register(String.class, JdbcType.CHAR, new StringTypeHandler());
        register(String.class, JdbcType.VARCHAR, new StringTypeHandler());


        // 处理object
        register(Object.class, new ObjectTypeHandler());
        register(HashMapTypeHandler.class, new ObjectTypeHandler());


        // 处理日期
        register(Date.class, new DateTypeHandler());
    }

    /**
     * 注册
     *
     * @param javaType
     * @param typeHandler
     * @param <T>
     */
    private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
        register(javaType, null, typeHandler);
    }

    /**
     * 注册
     *
     * @param javaType
     * @param jdbcType
     * @param handler
     */
    private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
        // 如果 javaType 不等于null
        if (null != javaType) {
            Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.computeIfAbsent(javaType, k -> new HashMap<>(1));
            map.put(jdbcType, handler);
        }
        // 给它处理
        ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
    }


    /**
     * 判断类型处理器是否存在
     *
     * @param javaType
     * @return
     */
    public boolean hasTypeHandler(Class<?> javaType) {
        return hasTypeHandler(javaType, null);
    }

    /**
     * 判断类型处理器是否存在
     *
     * @param javaType
     * @param jdbcType
     * @return
     */
    public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
        // 如果getTypeHandler为null 就是不存在
        return javaType != null && getTypeHandler(javaType, jdbcType) != null;
    }


    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
        return getTypeHandler((Type) type, jdbcType);
    }


    /**
     * 获取类型处理器
     *
     * @param type
     * @param jdbcType
     * @param <T>
     * @return
     */
    private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
        // 根据java类型获取对应的类型处理器
        Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);

        TypeHandler<?> handler = null;

        // 如果不为空
        if (jdbcHandlerMap != null) {
            // 根据 jdbc类型获取对应的类型处理器
            handler = jdbcHandlerMap.get(jdbcType);
            if (handler == null) {
                // 去获取一个javaType通用的类型处理器
                handler = jdbcHandlerMap.get(null);
            }
        }
        // type drives generics here，这里其实可能为空的
        return (TypeHandler<T>) handler;
    }


    public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
        return ALL_TYPE_HANDLERS_MAP.get(handlerType);
    }
}

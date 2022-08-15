package com.ytrue.orm.transaction.type;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/15 15:19
 * @description 类型别名注册机
 */
public class TypeAliasRegistry {

    private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<>();

    /**
     * 构造函数里注册系统内置的类型别名
     */
    public TypeAliasRegistry() {
        registerAlias("string", String.class);
        // 基本包装类型
        registerAlias("byte", Byte.class);
        registerAlias("long", Long.class);
        registerAlias("short", Short.class);
        registerAlias("int", Integer.class);
        registerAlias("integer", Integer.class);
        registerAlias("double", Double.class);
        registerAlias("float", Float.class);
        registerAlias("boolean", Boolean.class);
    }

    /**
     * 注册别名
     *
     * @param alias
     * @param value
     */
    public void registerAlias(String alias, Class<?> value) {
        String key = alias.toLowerCase(Locale.ENGLISH);
        TYPE_ALIASES.put(key, value);
    }

    /**
     * 获取别名
     *
     * @param string
     * @param <T>
     * @return
     */
    public <T> Class<T> resolveAlias(String string) {
        String key = string.toLowerCase(Locale.ENGLISH);
        return (Class<T>) TYPE_ALIASES.get(key);
    }
}

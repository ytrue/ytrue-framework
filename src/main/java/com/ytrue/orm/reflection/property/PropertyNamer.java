package com.ytrue.orm.reflection.property;

import java.util.Locale;

/**
 * @author ytrue
 * @date 2022/8/19 14:04
 * @description 属性命名器
 */
public class PropertyNamer {

    public static final String IS = "is";
    public static final String GET = "get";
    public static final String SET = "set";

    /**
     * 方法名称转换为属性名称
     */
    public static String methodToProperty(String name) {
        if (name.startsWith(IS)) {
            name = name.substring(2);
        } else if (name.startsWith(GET) || name.startsWith(SET)) {
            name = name.substring(3);
        } else {
            throw new RuntimeException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
        }

        // 如果只有1个字母，转换为小写
        // 如果大于1个字母，第二个字母非大写，转换为小写
        if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }

        return name;
    }


    /**
     * 开头判断get/set/is
     */
    public static boolean isProperty(String name) {
        return name.startsWith(GET) || name.startsWith(SET) || name.startsWith(IS);
    }

    /**
     * 是否为 getter
     */
    public static boolean isGetter(String name) {
        return name.startsWith(GET) || name.startsWith(IS);
    }

    /**
     * 是否为 setter
     */
    public static boolean isSetter(String name) {
        return name.startsWith(SET);
    }

}

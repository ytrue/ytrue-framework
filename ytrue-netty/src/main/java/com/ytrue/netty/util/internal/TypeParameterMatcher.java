package com.ytrue.netty.util.internal;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-08-14 14:29
 * @description 源码中的一个工具类，类型匹配器
 * 在SimpleChannelInboundHandler类中也会看到该类的对象，判断要处理的数据，也就是msg是不是和泛型对应
 * 现在各种类都引入了，所以SimpleChannelInboundHandler的功能也齐全了，大家可以去看一看
 */
public abstract class TypeParameterMatcher {
    /**
     * @Author: ytrue
     * @Description:这个属性的内部方法match返回为true，意思就是任何类型都可以匹配
     */
    private static final TypeParameterMatcher NOOP = new TypeParameterMatcher() {
        @Override
        public boolean match(Object msg) {
            return true;
        }
    };

    /**
     * @Author: ytrue
     * @Description:这个方法可以获得类型匹配器，具体逻辑会延伸到InternalThreadLocalMap中，大家可以点进去看一看
     * 类型匹配器创建成功后，会放回到InternalThreadLocalMap中，由此可以看出，类型匹配器实际上是每个单线程执行器私有的
     * 为什么要每个单线程执行器私有？因为每个单线程执行器是隔离的，各自管理很多channel，发送这些channel的消息，所以
     * 就给每个单线程执行器创建了一个类型匹配器，使用的时候直接去本地map中拿就可以
     */
    public static TypeParameterMatcher get(final Class<?> parameterType) {
        final Map<Class<?>, TypeParameterMatcher> getCache =
                InternalThreadLocalMap.get().typeParameterMatcherGetCache();
        TypeParameterMatcher matcher = getCache.get(parameterType);
        if (matcher == null) {
            //这里可以看见，如果泛型类型为Object，就返回一个默认返回true的类型匹配器
            if (parameterType == Object.class) {
                matcher = NOOP;
            } else {
                //这个就是创建了真正的类型匹配器
                matcher = new ReflectiveMatcher(parameterType);
            }
            //创建成功再放回到本地map中
            getCache.put(parameterType, matcher);
        }

        return matcher;
    }

    /**
     * @Author: ytrue
     * @Description:这个方法也会得到类型匹配器，逻辑和上面的get方法类似，但是这个方法没有传入编码器类的泛型
     * 所以要通过反射判断泛型类型为什么，会比get方法麻烦一些
     */
    public static TypeParameterMatcher find(
            final Object object, final Class<?> parametrizedSuperclass, final String typeParamName) {
        //如果是第一次获取类型匹配器，这里会得到一个map，当然，map中也是空的
        final Map<Class<?>, Map<String, TypeParameterMatcher>> findCache =
                InternalThreadLocalMap.get().typeParameterMatcherFindCache();
        final Class<?> thisClass = object.getClass();
        Map<String, TypeParameterMatcher> map = findCache.get(thisClass);
        //如果是第一次，得到的map不为null，只不过里面没有存储信息，而且这个map为IdentityHashMap类型的map
        if (map == null) {
            map = new HashMap<String, TypeParameterMatcher>();
            findCache.put(thisClass, map);
        }
        TypeParameterMatcher matcher = map.get(typeParamName);
        if (matcher == null) {
            //这里才开始创建类型匹配器，通过find0方法
            matcher = get(find0(object, parametrizedSuperclass, typeParamName));
            map.put(typeParamName, matcher);
        }

        return matcher;
    }
    //通过反射得到泛型的类型，然后创建类型匹配器，这个方法很长很麻烦，朋友们，我就不跟进去了，
    //大家有兴趣可以跟一跟。。
    private static Class<?> find0(
            final Object object, Class<?> parametrizedSuperclass, String typeParamName) {

        final Class<?> thisClass = object.getClass();
        Class<?> currentClass = thisClass;
        for (;;) {
            if (currentClass.getSuperclass() == parametrizedSuperclass) {
                int typeParamIndex = -1;
                TypeVariable<?>[] typeParams = currentClass.getSuperclass().getTypeParameters();
                for (int i = 0; i < typeParams.length; i ++) {
                    if (typeParamName.equals(typeParams[i].getName())) {
                        typeParamIndex = i;
                        break;
                    }
                }

                if (typeParamIndex < 0) {
                    throw new IllegalStateException(
                            "unknown type parameter '" + typeParamName + "': " + parametrizedSuperclass);
                }

                Type genericSuperType = currentClass.getGenericSuperclass();
                if (!(genericSuperType instanceof ParameterizedType)) {
                    return Object.class;
                }

                Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();

                Type actualTypeParam = actualTypeParams[typeParamIndex];
                if (actualTypeParam instanceof ParameterizedType) {
                    actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
                }
                if (actualTypeParam instanceof Class) {
                    return (Class<?>) actualTypeParam;
                }
                if (actualTypeParam instanceof GenericArrayType) {
                    Type componentType = ((GenericArrayType) actualTypeParam).getGenericComponentType();
                    if (componentType instanceof ParameterizedType) {
                        componentType = ((ParameterizedType) componentType).getRawType();
                    }
                    if (componentType instanceof Class) {
                        return Array.newInstance((Class<?>) componentType, 0).getClass();
                    }
                }
                if (actualTypeParam instanceof TypeVariable) {
                    // Resolved type parameter points to another type parameter.
                    TypeVariable<?> v = (TypeVariable<?>) actualTypeParam;
                    currentClass = thisClass;
                    if (!(v.getGenericDeclaration() instanceof Class)) {
                        return Object.class;
                    }

                    parametrizedSuperclass = (Class<?>) v.getGenericDeclaration();
                    typeParamName = v.getName();
                    if (parametrizedSuperclass.isAssignableFrom(thisClass)) {
                        continue;
                    } else {
                        return Object.class;
                    }
                }

                return fail(thisClass, typeParamName);
            }
            currentClass = currentClass.getSuperclass();
            if (currentClass == null) {
                return fail(thisClass, typeParamName);
            }
        }
    }

    private static Class<?> fail(Class<?> type, String typeParamName) {
        throw new IllegalStateException(
                "cannot determine the type of the type parameter '" + typeParamName + "': " + type);
    }

    public abstract boolean match(Object msg);

    /**
     * @Author: ytrue
     * @Description:静态内部类，这个类其实就承担了类型检查的职责
     */
    private static final class ReflectiveMatcher extends TypeParameterMatcher {
        private final Class<?> type;

        ReflectiveMatcher(Class<?> type) {
            this.type = type;
        }
        //这个就是检查是否匹配的方法
        @Override
        public boolean match(Object msg) {
            return type.isInstance(msg);
        }
    }

    TypeParameterMatcher() { }
}

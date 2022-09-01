package com.ytrue.orm.builder.annotation;

import com.ytrue.orm.annotations.Delete;
import com.ytrue.orm.annotations.Insert;
import com.ytrue.orm.annotations.Select;
import com.ytrue.orm.annotations.Update;
import com.ytrue.orm.binding.MapperMethod;
import com.ytrue.orm.builder.MapperBuilderAssistant;
import com.ytrue.orm.executor.keygen.Jdbc3KeyGenerator;
import com.ytrue.orm.executor.keygen.KeyGenerator;
import com.ytrue.orm.executor.keygen.NoKeyGenerator;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.ResultHandler;
import com.ytrue.orm.session.RowBounds;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author ytrue
 * @date 2022/8/30 16:04
 * @description 注解配置构建器 Mapper
 */
public class MapperAnnotationBuilder {

    /**
     * sql注解类型
     */
    private final Set<Class<? extends Annotation>> sqlAnnotationTypes = new HashSet<>();

    private Configuration configuration;
    private MapperBuilderAssistant assistant;
    private Class<?> type;

    public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
        // 把. 替换成 / 之后再加上 .java (best guess)
        String resource = type.getName().replace(".", "/") + ".java (best guess)";
        this.assistant = new MapperBuilderAssistant(configuration, resource);
        this.configuration = configuration;
        this.type = type;

        // 添加sql注解
        sqlAnnotationTypes.add(Select.class);
        sqlAnnotationTypes.add(Insert.class);
        sqlAnnotationTypes.add(Update.class);
        sqlAnnotationTypes.add(Delete.class);
    }

    public void parse() {
        // 转换字符串 interface com.ytrue.orm.xxxx.xxxDao
        String resource = type.toString();
        // 没有加载过走这里
        if (!configuration.isResourceLoaded(resource)) {
            // 设置命名空间，就是类名称
            assistant.setCurrentNamespace(type.getName());
            // 获取类上面的所有方法
            Method[] methods = type.getMethods();
            // 循环处理
            for (Method method : methods) {
                if (!method.isBridge()) {
                    // 解析语句
                    parseStatement(method);
                }
            }
        }
    }

    /**
     * 解析语句
     *
     * @param method
     */
    private void parseStatement(Method method) {
        // 获取这个方法的参数类型
        Class<?> parameterTypeClass = getParameterType(method);
        // 获取驱动，这里获取就是 XmlLanguageDriver
        LanguageDriver languageDriver = getLanguageDriver(method);
        // 获取方法上面的注解内容构建 SqlSource
        SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);

        if (sqlSource != null) {
            // 类名加方法名称 组成 mappedStatementId
            final String mappedStatementId = type.getName() + "." + method.getName();
            // 获取SqlCommandType
            SqlCommandType sqlCommandType = getSqlCommandType(method);


            // step-14 新增
            KeyGenerator keyGenerator;
            String keyProperty = "id";
            if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
                keyGenerator = configuration.isUseGeneratedKeys() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
            } else {
                keyGenerator = new NoKeyGenerator();
            }


            // 是否是查询
            boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

            String resultMapId = null;
            if (isSelect) {
                // 类名+方法名称+参数类型-参数类型
                resultMapId = parseResultMap(method);
            }

            // 调用助手类
            assistant.addMappedStatement(
                    mappedStatementId,
                    sqlSource,
                    sqlCommandType,
                    parameterTypeClass,
                    resultMapId,
                    getReturnType(method),
                    keyGenerator,
                    keyProperty,
                    languageDriver
            );
        }
    }

    /**
     * 解析 ResultMap
     *
     * @param method
     * @return
     */
    private String parseResultMap(Method method) {
        // generateResultMapName
        StringBuilder suffix = new StringBuilder();
        // 获取方法上面的参数类型
        for (Class<?> c : method.getParameterTypes()) {
            // 参数类型用 - 隔开
            suffix.append("-");
            suffix.append(c.getSimpleName());
        }
        // 如果没有参数 默认给一个 -void
        if (suffix.length() < 1) {
            suffix.append("-void");
        }
        // 类名+方法名称+参数类型-参数类型
        String resultMapId = type.getName() + "." + method.getName() + suffix;


        // 获取返回类型
        Class<?> returnType = getReturnType(method);
        // 添加 ResultMap
        assistant.addResultMap(resultMapId, returnType, new ArrayList<>());
        // 返回 类名+方法名称+参数类型-参数类型
        return resultMapId;
    }


    /**
     * 重点：DAO 方法的返回类型，如果为 List 则需要获取集合中的对象类型
     *
     * @param method
     * @return
     */
    private Class<?> getReturnType(Method method) {
        // 获取方法的返回类型
        Class<?> returnType = method.getReturnType();
        // 判断是不是集合类型
        if (Collection.class.isAssignableFrom(returnType)) {
            // 返回类型 List<User> 这样
            Type returnTypeParameter = method.getGenericReturnType();
            if (returnTypeParameter instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) returnTypeParameter).getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                    returnTypeParameter = actualTypeArguments[0];
                    if (returnTypeParameter instanceof Class) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        // (issue #443) actual type can be a also a parameterized type
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    } else if (returnTypeParameter instanceof GenericArrayType) {
                        Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
                        // (issue #525) support List<byte[]>
                        returnType = Array.newInstance(componentType, 0).getClass();
                    }
                }
            }
        }
        return returnType;
    }


    /**
     * 获取 SqlCommandType
     *
     * @param method
     * @return
     */
    private SqlCommandType getSqlCommandType(Method method) {
        // 获取方法注解的类型
        Class<? extends Annotation> type = getSqlAnnotationType(method);
        if (type == null) {
            return SqlCommandType.UNKNOWN;
        }
        // 获取注解名称，来获取 SqlCommandType
        return SqlCommandType.valueOf(type.getSimpleName().toUpperCase(Locale.ENGLISH));
    }

    /**
     * 从注释中获取 SqlSource
     *
     * @param method
     * @param parameterType
     * @param languageDriver
     * @return
     */
    private SqlSource getSqlSourceFromAnnotations(Method method, Class<?> parameterType, LanguageDriver languageDriver) {
        try {
            // 获取方法上面注解的类型
            Class<? extends Annotation> sqlAnnotationType = getSqlAnnotationType(method);
            // 如果有才会往下处理
            if (sqlAnnotationType != null) {
                // 获取注解的内容
                Annotation sqlAnnotation = method.getAnnotation(sqlAnnotationType);
                // 获取注解的value内容
                final String[] strings = (String[]) sqlAnnotation.getClass().getMethod("value").invoke(sqlAnnotation);
                // 构建 SqlSource
                return buildSqlSourceFromStrings(strings, parameterType, languageDriver);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Could not find value method on SQL annotation.  Cause: " + e);
        }
    }


    /**
     * 构建 SqlSource
     *
     * @param strings
     * @param parameterTypeClass
     * @param languageDriver
     * @return
     */
    private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
        final StringBuilder sql = new StringBuilder();
        for (String fragment : strings) {
            sql.append(fragment);
            sql.append(" ");
        }
        // 构建静态的SqlSource
        return languageDriver.createSqlSource(configuration, sql.toString(), parameterTypeClass);
    }


    /**
     * 获取方法上面注解的类型
     *
     * @param method
     * @return
     */
    private Class<? extends Annotation> getSqlAnnotationType(Method method) {
        for (Class<? extends Annotation> type : sqlAnnotationTypes) {
            // 获取上面的注解
            Annotation annotation = method.getAnnotation(type);
            if (annotation != null) {
                // 返回这个注解
                return type;
            }
        }
        return null;
    }

    /**
     * 获取默认的LanguageDriver
     *
     * @param method
     * @return
     */
    private LanguageDriver getLanguageDriver(Method method) {
        Class<?> langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
        return configuration.getLanguageRegistry().getDriver(langClass);
    }

    /**
     * 获取参数类型，这里其实要处理的
     *
     * @param method
     * @return
     */
    private Class<?> getParameterType(Method method) {
        Class<?> parameterType = null;
        // 获取方法上面所有参数的类型
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 循环参数
        for (Class<?> clazz : parameterTypes) {
            // 表示本class所代表的类/接口是否是cls所代表的类/接口的父类/父接口
            if (!RowBounds.class.isAssignableFrom(clazz) && !ResultHandler.class.isAssignableFrom(clazz)) {
                // 如果 parameterType 为 null  那么 parameterType 就是 这个参数类型
                if (parameterType == null) {
                    parameterType = clazz;
                } else {
                    // 有多个参数就是map类型
                    parameterType = MapperMethod.ParamMap.class;
                }
            }
        }
        // 返回参数类型
        return parameterType;
    }
}

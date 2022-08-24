package com.ytrue.orm.binding;

import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.SqlSession;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author ytrue
 * @date 2022/8/12 09:25
 * @description 映射器方法
 */
public class MapperMethod {

    private final SqlCommand command;
    private final MethodSignature method;

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration configuration) {
        this.command = new SqlCommand(configuration, mapperInterface, method);
        this.method = new MethodSignature(configuration, method);
    }

    /**
     * 执行方法
     *
     * @param sqlSession
     * @param args
     * @return
     */
    public Object execute(SqlSession sqlSession, Object[] args) {
        Object result = null;
        switch (command.getType()) {
            case INSERT:
                break;
            case DELETE:
                break;
            case UPDATE:
                break;
            case SELECT:
                Object param = method.convertArgsToSqlCommandParam(args);
                result = sqlSession.selectOne(command.getName(), param);
                break;
            default:
                throw new RuntimeException("Unknown execution method for: " + command.getName());
        }
        return result;
    }


    /**
     * 方法签名
     */
    public static class MethodSignature {

        private final SortedMap<Integer, String> params;

        public MethodSignature(Configuration configuration, Method method) {
            // 不可变的集合
            this.params = Collections.unmodifiableSortedMap(getParams(method));
        }

        /**
         * Args 转 SqlCommandParam
         * 1.没有参数返回null
         * 2.一个参数返回args[0]
         * 3.多个参数返回map 就会是 ["0"=>1,"1"=>"yangyi","param1"=>1,"param2"=>"yangyi"]
         *
         * @param args
         * @return
         */
        public Object convertArgsToSqlCommandParam(Object[] args) {
            final int paramCount = params.size();
            if (args == null || paramCount == 0) {
                // 如果没参数返回null
                return null;
            } else if (paramCount == 1) {
                // 如果只有一个参数，这里直接把参数值返回出去，比如 (1L) 这里就是 1L
                return args[params.keySet().iterator().next().intValue()];
            } else {
                // 否则，返回一个ParamMap，修改参数名，参数名就是其位置
                final Map<String, Object> param = new ParamMap<>();
                int i = 0;
                for (Map.Entry<Integer, String> entry : params.entrySet()) {
                    // 1.先加一个#{0},#{1},#{2}...参数
                    param.put(entry.getValue(), args[entry.getKey().intValue()]);
                    // issue #71, add param names as param1, param2...but ensure backward compatibility
                    final String genericParamName = "param" + (i + 1);
                    if (!param.containsKey(genericParamName)) {
                        /*
                         * 2.再加一个#{param1},#{param2}...参数
                         * 你可以传递多个参数给一个映射器方法。如果你这样做了,
                         * 默认情况下它们将会以它们在参数列表中的位置来命名,比如:#{param1},#{param2}等。
                         * 如果你想改变参数的名称(只在多参数情况下) ,那么你可以在参数上使用@Param(“paramName”)注解。
                         */
                        param.put(genericParamName, args[entry.getKey()]);
                    }
                    i++;
                }

                return param;
            }
        }


        /**
         * 获取方法的所有的参数
         *
         * @param method
         * @return
         */
        private SortedMap<Integer, String> getParams(Method method) {
            // 用一个TreeMap，这样就保证还是按参数的先后顺序
            final SortedMap<Integer, String> params = new TreeMap<>();
            // 获取方法上面的所有参数类型
            final Class<?>[] argTypes = method.getParameterTypes();
            // 存入
            for (int i = 0; i < argTypes.length; i++) {
                String paramName = String.valueOf(params.size());
                params.put(i, paramName);
            }
            return params;
        }
    }

    /**
     * 参数map，静态内部类,更严格的get方法，如果没有相应的key，报错
     */
    public static class ParamMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -2212268410512043556L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new RuntimeException("Parameter '" + key + "' not found. Available parameters are " + keySet());
            }
            return super.get(key);
        }

    }

    /**
     * SQL 指令
     */
    @Getter
    public static class SqlCommand {

        /**
         * 名称
         */
        private final String name;
        /**
         * sql类型
         */
        private final SqlCommandType type;

        /**
         * 构造方法 主要方法 构建 SqlCommand 给 name 和 type 赋值
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            // 获取 statementName  包名 + 类名 + 方法名称
            String statementName = mapperInterface.getName() + "." + method.getName();
            // 去map 里获取对应的 MappedStatement
            MappedStatement ms = configuration.getMappedStatement(statementName);
            // 赋值操作
            name = ms.getId();
            type = ms.getSqlCommandType();
        }
    }
}

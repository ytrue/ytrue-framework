package com.ytrue.orm.scripting.xmltags;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/9/2 14:15
 * @description 表达式求值器
 */
public class ExpressionEvaluator {

    /**
     * 表达式求布尔值，比如 username == 'ytrue'
     *
     * @param expression
     * @param parameterObject
     * @return
     */
    public boolean evaluateBoolean(String expression, Object parameterObject) {
        // 非常简单，就是调用 ognl
        Object value = OgnlCache.getValue(expression, parameterObject);
        if (value instanceof Boolean) {
            // 如果是Boolean
            return (Boolean) value;
        }
        if (value instanceof Number) {
            // 如果是Number，判断不为0
            return !new BigDecimal(String.valueOf(value)).equals(BigDecimal.ZERO);
        }
        // 否则判断不为null
        return value != null;
    }


    /**
     * foreach 调用，暂时用不上。解析表达式到一个Iterable 核心是ognl
     *
     * @param expression
     * @param parameterObject
     * @return
     */
    public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
        // 原生的ognl很强大，OgnlCache.getValue 直接就可以返回一个Iterable型或数组型或Map型了
        Object value = OgnlCache.getValue(expression, parameterObject);
        if (value == null) {
            throw new RuntimeException("The expression '" + expression + "' evaluated to a null value.");
        }
        if (value instanceof Iterable) {
            return (Iterable<?>) value;
        }
        if (value.getClass().isArray()) {
            // 如果是array，则把他变成一个List<Object>
            // 注释下面提到了，不能用Arrays.asList()，因为array可能是基本型，这样会出ClassCastException，
            // 见https://code.google.com/p/mybatis/issues/detail?id=209
            // the array may be primitive, so Arrays.asList() may throw
            // a ClassCastException (issue 209).  Do the work manually
            // Curse primitives! :) (JGB)
            int size = Array.getLength(value);
            List<Object> answer = new ArrayList<Object>();
            for (int i = 0; i < size; i++) {
                Object o = Array.get(value, i);
                answer.add(o);
            }
            return answer;
        }
        if (value instanceof Map) {
            return ((Map) value).entrySet();
        }
        throw new RuntimeException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
    }
}

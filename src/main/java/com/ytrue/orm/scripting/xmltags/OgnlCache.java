package com.ytrue.orm.scripting.xmltags;

import ognl.Ognl;
import ognl.OgnlException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author ytrue
 * @date 2022/9/2 14:22
 * @description OGNL缓存：http://code.google.com/p/mybatis/issues/detail?id=342
 * OGNL 是 Object-Graph Navigation Language 的缩写，它是一种功能强大的表达式语言（Expression Language，简称为EL）
 * 通过它简单一致的表达式语法，可以存取对象的任意属性，调用对象的方法，遍历整个对象的结构图，实现字段类型转化等功能。
 * 它使用相同的表达式去存取对象的属性。
 */
    public class OgnlCache {

    private static final Map<String, Object> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    private OgnlCache() {
        // Prevent Instantiation of Static Class
    }


    public static Object getValue(String expression, Object root) {
        try {
            Map<Object, OgnlClassResolver> context = Ognl.createDefaultContext(root, new OgnlClassResolver());
            return Ognl.getValue(parseExpression(expression), context, root);
        } catch (OgnlException e) {
            throw new RuntimeException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
        }
    }

    private static Object parseExpression(String expression) throws OgnlException {
        Object node = EXPRESSION_CACHE.get(expression);
        if (node == null) {
            // OgnlParser.topLevelExpression 操作耗时，加个缓存放到 ConcurrentHashMap 里面
            node = Ognl.parseExpression(expression);
            EXPRESSION_CACHE.put(expression, node);
        }
        return node;
    }

}

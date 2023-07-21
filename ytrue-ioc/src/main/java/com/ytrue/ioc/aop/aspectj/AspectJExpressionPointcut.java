package com.ytrue.ioc.aop.aspectj;

import com.ytrue.ioc.aop.ClassFilter;
import com.ytrue.ioc.aop.MethodMatcher;
import com.ytrue.ioc.aop.Pointcut;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ytrue
 * @date 2022/10/14 08:43
 * @description 切点表达式实现了 Pointcut、ClassFilter、MethodMatcher，三个接口定义方法，同时这个类主要是对 aspectj 包提供的表达式校验方法使用。
 */
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {


    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<>();

    static {
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
    }


    /**
     * 切点表达式
     */
    private final PointcutExpression pointcutExpression;

    public AspectJExpressionPointcut(String expression) {
        PointcutParser pointcutParser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES, this.getClass().getClassLoader());
        pointcutExpression = pointcutParser.parsePointcutExpression(expression);
    }

    /**
     * 类匹配
     *
     * @param clazz the candidate target class
     * @return
     */
    @Override
    public boolean matches(Class<?> clazz) {
        return pointcutExpression.couldMatchJoinPointsInType(clazz);
    }

    /**
     * 方法匹配
     *
     * @param method
     * @param targetClass
     * @return
     */
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return pointcutExpression.matchesMethodExecution(method).alwaysMatches();
    }

    @Override
    public ClassFilter getClassFilter() {
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return this;
    }
}

package com.ytrue.web.adapter;

import com.ytrue.web.annotation.ControllerAdvice;
import com.ytrue.web.annotation.ConvertType;
import com.ytrue.web.annotation.RequestMapping;
import com.ytrue.web.convert.*;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.handler.ServletInvocableMethod;
import com.ytrue.web.resolver.*;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author ytrue
 * @date 2023-12-15 11:26
 * @description 处理RequestMapping注解
 */
public class RequestMappingHandlerMethodAdapter extends ApplicationObjectSupport implements HandlerMethodAdapter, InitializingBean {

    private HandlerMethodArgumentResolverComposite resolverComposite = new HandlerMethodArgumentResolverComposite();

    private ConvertComposite convertComposite = new ConvertComposite();

    private HandlerMethodReturnValueHandlerComposite returnValueHandlerComposite = new HandlerMethodReturnValueHandlerComposite();
    private int order;

    @Override
    public boolean support(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequestMapping.class);
    }

    // 具体执行的
    @Override
    public void handler(HttpServletRequest req, HttpServletResponse res, HandlerMethod handler) throws Exception {
        final WebServletRequest webServletRequest = new WebServletRequest(req, res);
        final ServletInvocableMethod invocableMethod = new ServletInvocableMethod();
        invocableMethod.setHandlerMethod(handler);
        invocableMethod.setConvertComposite(convertComposite);
        invocableMethod.setReturnValueHandlerComposite(returnValueHandlerComposite);
        invocableMethod.setResolverComposite(resolverComposite);

        invocableMethod.invokeAndHandle(webServletRequest, handler);

    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }


    // 初始化基础组件
    @Override
    public void afterPropertiesSet() throws Exception {
        resolverComposite.addResolvers(getDefaultArgumentResolver());
        convertComposite.addConvertMap(getDefaultConvert());
        returnValueHandlerComposite.addMethodReturnValueHandlers(getDefaultMethodReturnValueHandler());

        // 获取用户的全局类型转换器
        final Map<Class, ConvertHandler> diyConvertMap = getDiyConvertMap();
        convertComposite.addConvertMap(diyConvertMap);
    }

    public Map<Class, ConvertHandler> getDiyConvertMap() {
        final HashMap<Class, ConvertHandler> convertHandlerHashMap = new HashMap<>();
        final ApplicationContext context = obtainApplicationContext();
        final String[] names = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(context, ControllerAdvice.class);
        for (String name : names) {
            final Class<?> type = context.getType(name);
            final Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                if (AnnotatedElementUtils.hasAnnotation(method, ConvertType.class)) {
                    final ConvertType convertType = AnnotatedElementUtils.findMergedAnnotation(method, ConvertType.class);
                    convertHandlerHashMap.put(convertType.value(), new ConvertHandler(context.getBean(name), method));
                }
            }
        }
        return convertHandlerHashMap;
    }


    // 初始化返回值处理器
    public List<HandlerMethodReturnValueHandler> getDefaultMethodReturnValueHandler() {
        final ArrayList<HandlerMethodReturnValueHandler> handlerMethodReturnValueHandlers = new ArrayList<>();
        handlerMethodReturnValueHandlers.add(new RequestResponseBodyMethodReturnValueHandler());

        return handlerMethodReturnValueHandlers;
    }

    // 初始化类型转换器
    public Map<Class, ConvertHandler> getDefaultConvert() {
        final Map<Class, ConvertHandler> convertMap = new HashMap<>();
        convertMap.put(Integer.class, getConvertHandler(new IntegerConvert(Integer.class)));
        convertMap.put(int.class, getConvertHandler(new IntegerConvert(Integer.class)));
        convertMap.put(String.class, getConvertHandler(new StringConvert(String.class)));
        convertMap.put(Long.class, getConvertHandler(new LongConvert(Long.class)));
        convertMap.put(long.class, getConvertHandler(new LongConvert(Long.class)));
        convertMap.put(Float.class, getConvertHandler(new FloatConvert(Float.class)));
        convertMap.put(float.class, getConvertHandler(new FloatConvert(Float.class)));
        convertMap.put(Boolean.class, getConvertHandler(new BooleanConvert(Boolean.class)));
        convertMap.put(boolean.class, getConvertHandler(new BooleanConvert(Boolean.class)));
        convertMap.put(Byte.class, getConvertHandler(new ByteConvert(Byte.class)));
        convertMap.put(byte.class, getConvertHandler(new ByteConvert(Byte.class)));
        convertMap.put(Short.class, getConvertHandler(new ShortConvert(Short.class)));
        convertMap.put(short.class, getConvertHandler(new ShortConvert(Short.class)));
        convertMap.put(Date.class, getConvertHandler(new DateConvert(Date.class)));
        convertMap.put(Map.class, getConvertHandler(new MapConvert(HashMap.class)));
        convertMap.put(Collection.class, getConvertHandler(new CollectionConvert(Collection.class)));
        convertMap.put(List.class, getConvertHandler(new ListConvert(ArrayList.class)));
        convertMap.put(Set.class, getConvertHandler(new SetConvert(HashSet.class)));
        return convertMap;
    }

    protected ConvertHandler getConvertHandler(Convert convert) {
        try {
            final Method method = convert.getClass().getDeclaredMethod("convert", Object.class);
            return new ConvertHandler(convert, method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<HandlerMethodArgumentResolver> getDefaultArgumentResolver() {
        final List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        resolvers.add(new PathVariableMethodArgumentResolver());
        resolvers.add(new PathVariableMapMethodArgumentResolver());
        resolvers.add(new RequestCookieMethodArgumentResolver());
        resolvers.add(new RequestHeaderMethodArgumentResolver());
        resolvers.add(new RequestHeaderMapMethodArgumentResolver());
        resolvers.add(new RequestPartMethodArgumentResolver());
        resolvers.add(new RequestParamMapMethodArgumentResolver());
        resolvers.add(new RequestParamMethodArgumentResolver());
        resolvers.add(new RequestRequestBodyMethodArgumentResolver());
        resolvers.add(new ServletResponseMethodArgumentResolver());
        resolvers.add(new ServletRequestMethodArgumentResolver());
        return resolvers;
    }

}

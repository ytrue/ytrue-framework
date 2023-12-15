package com.ytrue.web.handler;

import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.excpetion.NotFoundException;
import com.ytrue.web.resolver.HandlerMethodArgumentResolverComposite;
import com.ytrue.web.resolver.HandlerMethodReturnValueHandlerComposite;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-12-15 14:07
 * @description 用于执行HandlerMethod, 也会保存执行HandlerMethod的基础组件
 */
public class ServletInvocableMethod extends HandlerMethod{

    private HandlerMethod handlerMethod;

    private HandlerMethodArgumentResolverComposite resolverComposite = new HandlerMethodArgumentResolverComposite();

    private ConvertComposite convertComposite = new ConvertComposite();

    private HandlerMethodReturnValueHandlerComposite returnValueHandlerComposite = new HandlerMethodReturnValueHandlerComposite();

    private ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();



    public ServletInvocableMethod(Object bean, Method method) {
        super(bean, method);
    }
    public ServletInvocableMethod(){

    }

    public void setReturnValueHandlerComposite(HandlerMethodReturnValueHandlerComposite returnValueHandlerComposite) {
        this.returnValueHandlerComposite = returnValueHandlerComposite;
    }


    public void setHandlerMethod(HandlerMethod handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    public void setResolverComposite(HandlerMethodArgumentResolverComposite resolverComposite) {
        this.resolverComposite = resolverComposite;
    }

    public void setConvertComposite(ConvertComposite convertComposite) {
        this.convertComposite = convertComposite;
    }


    public void invokeAndHandle(WebServletRequest webServletRequest, HandlerMethod handler, Object... providerArgs) throws Exception {

        // 1.获取参数
        final Object[] methodArguments = getMethodArguments(webServletRequest, handler,providerArgs);
        // 2.执行
        final Object returnValue = doInvoke(methodArguments);
        // 3.选择返回值处理器，处理执行后的返回值
        this.returnValueHandlerComposite.doInvoke(returnValue,handler.getMethod(),webServletRequest);
    }

    public Object[] getMethodArguments(WebServletRequest webServletRequest,HandlerMethod handlerMethod,Object... providerArgs) throws Exception {
        final MethodParameter[] parameters = handlerMethod.getParameters();
        Object args[] = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(nameDiscoverer);

            args[i] = findProviderArgs(parameter,providerArgs);
            if (args[i]!=null){
                continue;
            }
            // 参数解析器
            if (!this.resolverComposite.supportsParameter(parameter)) {
                throw new NotFoundException("没有参数解析器解析参数:" + parameter.getParameterType());
            }
            args[i] = this.resolverComposite.resolveArgument(parameter,handlerMethod,webServletRequest,this.convertComposite);
        }
        return args;
    }

    private Object findProviderArgs(MethodParameter parameter, Object[] providerArgs) {

        final Class<?> parameterType = parameter.getParameterType();
        // 遍历参数，参数的
        for (Object providerArg : providerArgs) {
            if (parameterType.isInstance(providerArg)){
                return providerArg;
            }
        }
        return null;
    }



    public Object doInvoke(Object args[]) throws Exception {

        final Object returnValue = this.handlerMethod.getMethod().invoke(this.handlerMethod.getBean(), args);

        return returnValue;
    }
}

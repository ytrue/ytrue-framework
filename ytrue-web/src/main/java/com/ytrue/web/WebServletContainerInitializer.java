package com.ytrue.web;

import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-12-15 10:02
 * @description springmvc 容器初始化
 */
@HandlesTypes(WebApplicationInitializer.class)
public class WebServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> webApplications, ServletContext ctx) throws ServletException {

        // 判断webApplications是否为空
        if (!ObjectUtils.isEmpty(webApplications)) {
            // 获取有多少个
            final ArrayList<WebApplicationInitializer> initializers = new ArrayList<>(webApplications.size());

            // 循环
            for (Class<?> webApplication : webApplications) {

                // 就是判断是不是 WebApplicationInitializer
                if (!webApplication.isInterface() && !Modifier.isAbstract(webApplication.getModifiers())
                    && WebApplicationInitializer.class.isAssignableFrom(webApplication)) {
                    try {
                        // 反射创建，加入initializers
                        initializers.add((WebApplicationInitializer) ReflectionUtils.accessibleConstructor(webApplication).newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // 调用onStartUp()方法
            if (!ObjectUtils.isEmpty(initializers)) {
                for (WebApplicationInitializer initializer : initializers) {
                    initializer.onStartUp(ctx);
                }
            }
        }

    }
}

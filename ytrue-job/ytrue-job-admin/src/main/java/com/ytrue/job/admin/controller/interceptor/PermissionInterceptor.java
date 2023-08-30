package com.ytrue.job.admin.controller.interceptor;

import com.ytrue.job.admin.controller.annotation.PermissionLimit;
import com.ytrue.job.admin.core.model.XxlJobUser;
import com.ytrue.job.admin.core.util.I18nUtil;
import com.ytrue.job.admin.service.LoginService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-08-29 9:31
 * @description 配合权限注解使用的
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

    @Resource
    private LoginService loginService;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 是否需要登录 默认true
        boolean needLogin = true;
        // 是否需要超级管理员 默认fasel
        boolean needAdminuser = false;
        HandlerMethod method = (HandlerMethod) handler;
        // 获取方法上面的注解
        PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
        // 如果不为空，就获取注解的内容，赋值
        if (permission != null) {
            needLogin = permission.limit();
            needAdminuser = permission.adminuser();
        }
        // 判断是否需要登录
        if (needLogin) {
            // 获取用户信息
            XxlJobUser loginUser = loginService.ifLogin(request, response);
            // 获取不到就跳转登录页面
            if (loginUser == null) {
                response.setStatus(302);
                response.setHeader("location", request.getContextPath() + "/toLogin");
                return false;
            }
            // 判断是否有权限
            if (needAdminuser && loginUser.getRole() != 1) {
                // 权限拦截
                throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
            }
            // 设置请求属性，把用户信息放入进去
            request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
        }
        return true;
    }

}

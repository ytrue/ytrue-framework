package com.ytrue.job.admin.controller.interceptor;

import com.ytrue.job.admin.core.util.FtlUtil;
import com.ytrue.job.admin.core.util.I18nUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @author ytrue
 * @date 2023-08-29 9:30
 * @description CookieInterceptor
 */
@Component
public class CookieInterceptor implements AsyncHandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView != null && request.getCookies() != null && request.getCookies().length > 0) {
            HashMap<String, Cookie> cookieMap = new HashMap<>();
            for (Cookie ck : request.getCookies()) {
                cookieMap.put(ck.getName(), ck);
            }
            modelAndView.addObject("cookieMap", cookieMap);
        }
        if (modelAndView != null) {
            //FtlUtil.generateStaticModel(I18nUtil.class.getName()这行代码的意思是把国际化工具类存入freemarker的模版信息中
            modelAndView.addObject("I18nUtil", FtlUtil.generateStaticModel(I18nUtil.class.getName()));
        }

    }

}

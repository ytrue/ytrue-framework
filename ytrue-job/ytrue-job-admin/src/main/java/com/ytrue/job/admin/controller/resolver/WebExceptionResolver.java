package com.ytrue.job.admin.controller.resolver;

import com.ytrue.job.admin.core.exception.XxlJobException;
import com.ytrue.job.admin.core.util.JacksonUtil;
import com.ytrue.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ytrue
 * @date 2023-08-30 9:38
 * @description 异常解析器
 */
@Component
public class WebExceptionResolver implements HandlerExceptionResolver {

    private static final transient Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(ex instanceof XxlJobException)) {
            logger.error("WebExceptionResolver:{}", ex);
        }
        boolean isJson = false;
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
            if (responseBody != null) {
                isJson = true;
            }
        }
        ReturnT<String> errorResult = new ReturnT<>(ReturnT.FAIL_CODE, ex.toString().replaceAll("\n", "<br/>"));
        ModelAndView mv = new ModelAndView();
        if (isJson) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print(JacksonUtil.writeValueAsString(errorResult));
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            mv.addObject("exceptionMsg", errorResult.getMsg());
            mv.setViewName("/common/common.exception");
        }
        return mv;
    }
}

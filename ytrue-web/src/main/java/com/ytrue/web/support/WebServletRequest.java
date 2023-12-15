package com.ytrue.web.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-12-15 13:33
 * @description WebServletRequest
 */
public class WebServletRequest {

    final HttpServletRequest request;

    final HttpServletResponse response;


    public WebServletRequest(HttpServletRequest request,HttpServletResponse response){
        this.request = request;
        this.response = response;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }
}

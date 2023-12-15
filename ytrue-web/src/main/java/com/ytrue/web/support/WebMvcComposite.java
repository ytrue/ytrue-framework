package com.ytrue.web.support;

import com.ytrue.web.intercpetor.InterceptorRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-12-15 13:35
 * @description WebMvcComposite
 */
public class WebMvcComposite implements WebMvcConfigurer {

    private List<WebMvcConfigurer> webMvcConfigurers = new ArrayList<>();

    public void addWebMvcConfigurers(List<WebMvcConfigurer> webMvcConfigurers) {
        this.webMvcConfigurers.addAll(webMvcConfigurers);
    }


    @Override
    public void addIntercept(InterceptorRegistry registry) {
        for (WebMvcConfigurer webMvcConfigurer : webMvcConfigurers) {
            webMvcConfigurer.addIntercept(registry);
        }
    }
}



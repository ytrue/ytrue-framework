package com.ytrue.web.support;

import com.ytrue.web.intercpetor.InterceptorRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-12-15 14:26
 * @description DelegatingWebMvcConfiguration
 */
public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport {


    private WebMvcComposite webMvcComposite = new WebMvcComposite();

    @Autowired(required = false)
    public void setWebMvcComposite(List<WebMvcConfigurer> webMvcConfigurers) {
        webMvcComposite.addWebMvcConfigurers(webMvcConfigurers);

    }

    @Override
    protected void getIntercept(InterceptorRegistry registry) {
        webMvcComposite.addIntercept(registry);
    }
}

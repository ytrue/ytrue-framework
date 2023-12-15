package com.ytrue.web.test.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author ytrue
 * @date 2023-12-15 9:37
 * @description AppConfig
 */
@Configuration
@ComponentScan("com.ytrue.web.test.beans")
public class AppConfig {


    @Bean
    public User user() {
        User user = new User();
        user.setAge(1);
        user.setName("test");
        return user;
    }
}

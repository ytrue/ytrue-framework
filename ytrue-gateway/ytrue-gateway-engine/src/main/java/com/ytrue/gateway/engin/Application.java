package com.ytrue.gateway.engin;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author ytrue
 * @date 2023-09-09 9:46
 * @description Application
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@Configurable
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

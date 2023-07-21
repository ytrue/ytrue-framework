package com.ytrue.middleware.orm.spring;

import com.ytrue.orm.io.Resources;
import com.ytrue.orm.session.SqlSessionFactory;
import com.ytrue.orm.session.SqlSessionFactoryBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.Reader;

/**
 * @author ytrue
 * @date 2022/9/10 12:14
 * @description 会话工厂对象
 */
public class SqlSessionFactoryBean implements InitializingBean, FactoryBean<SqlSessionFactory> {

    private String resource;
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public SqlSessionFactory getObject() throws Exception {
        return sqlSessionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return SqlSessionFactory.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 读取文件
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
}

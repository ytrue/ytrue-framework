package com.ytrue.orm.session;

import com.ytrue.orm.builder.xml.XMLConfigBuilder;
import com.ytrue.orm.session.defaults.DefaultSqlSessionFactory;

import java.io.Reader;

/**
 * @author ytrue
 * @date 2022/8/12 09:47
 * @description SqlSessionFactoryBuilder
 */
public class SqlSessionFactoryBuilder {

    public SqlSessionFactory build(Reader reader) {
        XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(reader);
        return build(xmlConfigBuilder.parse());
    }

    public SqlSessionFactory build(Configuration config) {
        return new DefaultSqlSessionFactory(config);
    }
}

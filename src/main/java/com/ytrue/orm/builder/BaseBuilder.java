package com.ytrue.orm.builder;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.TypeAliasRegistry;
import com.ytrue.orm.type.TypeHandlerRegistry;

/**
 * @author ytrue
 * @date 2022/8/11 16:26
 * @description 构建器的基类，建造者模式
 */
public class BaseBuilder {

    protected final TypeAliasRegistry typeAliasRegistry;
    protected final Configuration configuration;
    protected final TypeHandlerRegistry typeHandlerRegistry;

    public BaseBuilder(Configuration configuration) {
        this.configuration = configuration;
        this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
        this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
    }

    public Configuration getConfiguration() {
        return configuration;
    }



    protected Class<?> resolveAlias(String alias) {
        return typeAliasRegistry.resolveAlias(alias);
    }
}

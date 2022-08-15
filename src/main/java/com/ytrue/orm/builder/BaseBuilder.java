package com.ytrue.orm.builder;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.transaction.type.TypeAliasRegistry;

/**
 * @author ytrue
 * @date 2022/8/11 16:26
 * @description 构建器的基类，建造者模式
 */
public class BaseBuilder {

    protected final TypeAliasRegistry typeAliasRegistry;
    protected final Configuration configuration;

    public BaseBuilder(Configuration configuration) {
        this.configuration = configuration;
        this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}

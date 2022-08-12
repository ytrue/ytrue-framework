package com.ytrue.orm.builder;

import com.ytrue.orm.session.Configuration;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2022/8/11 16:26
 * @description 构建器的基类，建造者模式
 */
@AllArgsConstructor
@Getter
public class BaseBuilder {

    protected final Configuration configuration;
}

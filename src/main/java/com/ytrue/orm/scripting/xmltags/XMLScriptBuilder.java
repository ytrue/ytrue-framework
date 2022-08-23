package com.ytrue.orm.scripting.xmltags;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.defaults.RawSqlSource;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/23 09:43
 * @description XMLScriptBuilder
 */
public class XMLScriptBuilder extends BaseBuilder {

    /**
     * (select|insert|update|delete)
     */
    private Element element;

    /**
     * 是否动态
     */
    private boolean isDynamic;

    /**
     * 参数类型
     */
    private Class<?> parameterType;

    public XMLScriptBuilder(Configuration configuration, Element element, Class<?> parameterType) {
        super(configuration);
        this.element = element;
        this.parameterType = parameterType;
    }

    public SqlSource parseScriptNode() {
        List<SqlNode> contents = parseDynamicTags(element);
        MixedSqlNode rootSqlNode = new MixedSqlNode(contents);

        return new RawSqlSource(configuration, rootSqlNode, parameterType);
    }


    List<SqlNode> parseDynamicTags(Element element) {
        List<SqlNode> contents = new ArrayList<>();
        // element.getText 拿到 SQL
        String data = element.getText();
        contents.add(new StaticTextSqlNode(data));
        return contents;
    }
}

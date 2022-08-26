package com.ytrue.orm.builder.xml;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.builder.MapperBuilderAssistant;
import com.ytrue.orm.io.Resources;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/23 09:15
 * @description 提供单独的 XML 映射构建器 XMLMapperBuilder 类，
 * 把关于 Mapper 内的 SQL 进行解析处理。提供了这个类以后，
 * 就可以把这个类的操作放到 XML 配置构建器，
 * XMLConfigBuilder#mapperElement 中进行使用了
 */
public class XMLMapperBuilder extends BaseBuilder {


    private final static String EMPTY_STRING = "";

    /**
     * mapper标签
     */
    private Element element;

    /**
     * 资源路径
     */
    private String resource;


    /**
     * 映射器构建助手
     */
    private MapperBuilderAssistant builderAssistant;

    public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource) throws DocumentException {
        this(new SAXReader().read(inputStream), configuration, resource);
    }

    public XMLMapperBuilder(Document document, Configuration configuration, String resource) {
        super(configuration);
        this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
        this.element = document.getRootElement();
        this.resource = resource;
    }

    /**
     * 解析xml
     */
    public void parse() throws ClassNotFoundException {
        // 如果当前资源没有加载过再加载，防止重复加载
        if (!configuration.isResourceLoaded(resource)) {
            configurationElement(element);
            // 标记一下，已经加载过了
            configuration.addLoadedResource(resource);
            // 绑定映射器到namespace
            configuration.addMapper(Resources.classForName(builderAssistant.getCurrentNamespace()));
        }
    }

    /**
     * 配置mapper元素
     *
     * @param element
     */
    private void configurationElement(Element element) {
        // 1.配置namespace
        String currentNamespace = element.attributeValue("namespace");
        if (currentNamespace.equals(EMPTY_STRING)) {
            throw new RuntimeException("Mapper's namespace cannot be empty");
        }

        builderAssistant.setCurrentNamespace(currentNamespace);

        // 2.配置select|insert|update|delete
        buildStatementFromContext(element.elements("select"));
    }

    /**
     * 配置select|insert|update|delete
     *
     * @param list
     */
    private void buildStatementFromContext(List<Element> list) {
        for (Element element : list) {
            final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, element);
            statementParser.parseStatementNode();
        }
    }
}

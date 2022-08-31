package com.ytrue.orm.builder.xml;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.builder.MapperBuilderAssistant;
import com.ytrue.orm.builder.ResultMapResolver;
import com.ytrue.orm.io.Resources;
import com.ytrue.orm.mapping.ResultFlag;
import com.ytrue.orm.mapping.ResultMap;
import com.ytrue.orm.mapping.ResultMapping;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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


        // 2. 解析resultMap step-13 新增
        resultMapElements(element.elements("resultMap"));

        // 2.配置select|insert|update|delete
        buildStatementFromContext(element.elements("select"),
                element.elements("insert"),
                element.elements("update"),
                element.elements("delete")
        );
    }

    /**
     * 解析resultMap 标签
     *
     * @param list
     */
    private void resultMapElements(List<Element> list) {
        for (Element element : list) {
            try {
                resultMapElement(element, Collections.emptyList());
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 解析resultMap 标签
     *
     * <resultMap id="activityMap" type="com.ytrue.orm.test.po.Activity">
     * <id column="id" property="id"/>
     * <result column="activity_id" property="activityId"/>
     * <result column="activity_name" property="activityName"/>
     * <result column="activity_desc" property="activityDesc"/>
     * <result column="create_time" property="createTime"/>
     * <result column="update_time" property="updateTime"/>
     * </resultMap>
     */
    private ResultMap resultMapElement(Element resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
        // 获取id
        String id = resultMapNode.attributeValue("id");
        // 获取对象类型
        String type = resultMapNode.attributeValue("type");
        // 构建type的 class
        Class<?> typeClass = resolveClass(type);

        List<ResultMapping> resultMappings = new ArrayList<>();
        resultMappings.addAll(additionalResultMappings);

        // 获取这个元素的所有子元素
        List<Element> resultChildren = resultMapNode.elements();

        // 循环处理
        for (Element resultChild : resultChildren) {
            List<ResultFlag> flags = new ArrayList<>();
            // 如果是id就加入id 的标识
            if ("id".equals(resultChild.getName())) {
                flags.add(ResultFlag.ID);
            }

            // 构建 ResultMapping
            resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
        }

        // 创建结果映射解析器
        ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, resultMappings);
        return resultMapResolver.resolve();
    }

    /**
     * <id column="id" property="id"/>
     * <result column="activity_id" property="activityId"/>
     */
    private ResultMapping buildResultMappingFromContext(Element context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
        // 获取 property 属性
        String property = context.attributeValue("property");
        // 获取 column 属性
        String column = context.attributeValue("column");
        return builderAssistant.buildResultMapping(resultType, property, column, flags);
    }


    /**
     * 被@SafeVarargs注解标注的方法必须是由static或者final修饰的方法
     * 配置select|insert|update|delete
     *
     * @param lists
     */
    @SafeVarargs
    private final void buildStatementFromContext(List<Element>... lists) {
        for (List<Element> list : lists) {
            for (Element element : list) {
                final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, element);
                statementParser.parseStatementNode();
            }
        }
    }
}

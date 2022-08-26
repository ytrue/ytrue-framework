package com.ytrue.orm.builder.xml;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.datasource.DataSourceFactory;
import com.ytrue.orm.io.Resources;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.Environment;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.transaction.TransactionFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ytrue
 * @date 2022/8/11 16:26
 * @description XML配置构建器，建造者模式，继承BaseBuilder
 */
public class XMLConfigBuilder extends BaseBuilder {

    private Element root;


    /**
     * 传入xml的read 通过dom4j 解析xml，获取xml的根元素 赋值给 root
     *
     * @param reader
     */
    public XMLConfigBuilder(Reader reader) {
        // 1. 调用父类初始化Configuration
        super(new Configuration());
        // 2. dom4j 处理 xml,mybatis是对dom4j做了封装了这里就不做了直接使用
        SAXReader saxReader = new SAXReader();
        try {
            // 获取xml 获取document
            Document document = saxReader.read(new InputSource(reader));
            // 获取根元素
            root = document.getRootElement();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }


    /**
     * 解析root，封装成 Configuration 对象
     * 解析配置；类型别名、插件、对象工厂、对象包装工厂、设置、环境、类型转换、映射器
     *
     * @return Configuration
     */
    public Configuration parse() {
        try {
            // 获取 xml下面的 environments元素，解析
            environmentsElement(root.element("environments"));
            // 获取 xml下面的 mappers元素，解析
            mapperElement(root.element("mappers"));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
        return configuration;
    }


    /**
     * 解析environments标签
     *
     * @param context
     * @throws Exception
     */
    private void environmentsElement(Element context) throws Exception {

        // 获取 environments 的 default 属性
        String environment = context.attributeValue("default");

        // 获取所有的 environment 标签
        List<Element> environmentList = context.elements("environment");

        // 循环处理
        for (Element e : environmentList) {
            // 如果 environments default="development" 和 environment id="development" 相等
            if (environment.equals(e.attributeValue("id"))) {
                // 事务管理器 这里获取的是 JdbcTransactionFactory
                TransactionFactory txFactory = (TransactionFactory) typeAliasRegistry.
                        resolveAlias(e.element("transactionManager").attributeValue("type")).newInstance();

                // 获取数据源 这里获取的是 DruidDataSourceFactory
                Element dataSourceElement = e.element("dataSource");
                DataSourceFactory dataSourceFactory = (DataSourceFactory) typeAliasRegistry
                        .resolveAlias(dataSourceElement.attributeValue("type")).newInstance();

                // 获取下面property标签的属性
                List<Element> propertyList = dataSourceElement.elements("property");

                Properties props = new Properties();
                for (Element property : propertyList) {
                    props.setProperty(property.attributeValue("name"), property.attributeValue("value"));
                }
                // 获取数据源
                dataSourceFactory.setProperties(props);
                DataSource dataSource = dataSourceFactory.getDataSource();

                // 构建环境
                configuration.setEnvironment(Environment.builder()
                        .transactionFactory(txFactory)
                        .dataSource(dataSource)
                        .build());

            }
        }
    }


    /**
     * 解析mapper
     *
     * <mappers>
     * <mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
     * <mapper resource="org/mybatis/builder/BlogMapper.xml"/>
     * <mapper resource="org/mybatis/builder/PostMapper.xml"/>
     * </mappers>
     *
     * @param mappers
     * @throws Exception
     */
    private void mapperElement(Element mappers) throws Exception {
        // 获取mapper标签，有多个循环
        List<Element> mapperList = mappers.elements("mapper");
        for (Element e : mapperList) {
            // 获取mapper标签的 resource 属性
            String resource = e.attributeValue("resource");
            // 再去解析 mapper resource 属性对应的xml文件的内容
            InputStream inputStream = Resources.getResourceAsStream(resource);

            // 在for循环里每个mapper都重新new一个XMLMapperBuilder，来解析
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource);
            mapperParser.parse();
        }
    }


}

package com.ytrue.orm.builder.xml;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.builder.MapperBuilderAssistant;
import com.ytrue.orm.executor.keygen.Jdbc3KeyGenerator;
import com.ytrue.orm.executor.keygen.KeyGenerator;
import com.ytrue.orm.executor.keygen.NoKeyGenerator;
import com.ytrue.orm.executor.keygen.SelectKeyGenerator;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Element;

import java.util.List;
import java.util.Locale;

/**
 * @author ytrue
 * @date 2022/8/23 09:25
 * @description XMLStatementBuilder
 */
public class XMLStatementBuilder extends BaseBuilder {


    /**
     * 映射构建器助手
     */
    private MapperBuilderAssistant builderAssistant;


    /**
     * 这是是 select ，update,delete, insert 这样的标签
     */
    private Element element;


    public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, Element element) {
        super(configuration);
        this.builderAssistant = builderAssistant;
        this.element = element;
    }


    /**
     * 解析语句(select|insert|update|delete)
     * <select
     * id="selectPerson"
     * parameterType="int"
     * parameterMap="deprecated"
     * resultType="hashmap"
     * resultMap="personResultMap"
     * flushCache="false"
     * useCache="true"
     * timeout="10000"
     * fetchSize="256"
     * statementType="PREPARED"
     * resultSetType="FORWARD_ONLY">
     * SELECT * FROM PERSON WHERE ID = #{id}
     * </select>
     */
    public void parseStatementNode() {
        // 获取 id值
        String id = element.attributeValue("id");
        // 获取参数的类型
        String parameterType = element.attributeValue("parameterType");
        Class<?> parameterTypeClass = resolveAlias(parameterType);

        // 获取返回类型
        String resultType = element.attributeValue("resultType");
        Class<?> resultTypeClass = resolveAlias(resultType);

        // 外部应用 resultMap
        String resultMap = element.attributeValue("resultMap");


        // 获取命令类型(select|insert|update|delete)
        String nodeName = element.getName();
        // 获取 SQL 指令类型
        SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));

        // 获取默认语言驱动器
        Class<?> langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
        LanguageDriver langDriver = configuration.getLanguageRegistry().getDriver(langClass);

        // 解析<selectKey> step-14 新增
        processSelectKeyNodes(id, parameterTypeClass, langDriver);

        // 解析成SqlSource，DynamicSqlSource/RawSqlSource
        SqlSource sqlSource = langDriver.createSqlSource(configuration, element, parameterTypeClass);


        // 属性标记【仅对 insert 有用】, MyBatis 会通过 getGeneratedKeys 或者通过 insert 语句的 selectKey 子元素设置它的值 step-14 新增
        String keyProperty = element.attributeValue("keyProperty");

        KeyGenerator keyGenerator = null;
        String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
        keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);

        // 一般会走这个
        if (configuration.hasKeyGenerator(keyStatementId)) {
            keyGenerator = configuration.getKeyGenerator(keyStatementId);
        } else {
            keyGenerator = configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType) ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
        }


        // 调用助手类
        builderAssistant.addMappedStatement(id,
                sqlSource,
                sqlCommandType,
                parameterTypeClass,
                resultMap,
                resultTypeClass,
                keyGenerator,
                keyProperty,
                langDriver);
    }

    /**
     * 解析<selectKey>
     *
     * @param id
     * @param parameterTypeClass
     * @param langDriver
     */
    private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
        List<Element> selectKeyNodes = element.elements("selectKey");
        parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver);
    }

    private void parseSelectKeyNodes(String parentId, List<Element> list, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
        for (Element nodeToHandle : list) {
            // id + !selectKey
            String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
            parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, languageDriver);
        }
    }

    /**
     * <selectKey keyProperty="id" order="AFTER" resultType="long">
     * SELECT LAST_INSERT_ID()
     * </selectKey>
     *
     * @param id
     * @param nodeToHandle
     * @param parameterTypeClass
     * @param langDriver
     */
    private void parseSelectKeyNode(String id, Element nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver) {
        String resultType = nodeToHandle.attributeValue("resultType");
        Class<?> resultTypeClass = resolveClass(resultType);
        // 是否再之前执行
        boolean executeBefore = "BEFORE".equals(nodeToHandle.attributeValue("order", "AFTER"));
        String keyProperty = nodeToHandle.attributeValue("keyProperty");

        // default
        String resultMap = null;
        KeyGenerator keyGenerator = new NoKeyGenerator();

        // 解析成SqlSource，DynamicSqlSource/RawSqlSource
        SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
        SqlCommandType sqlCommandType = SqlCommandType.SELECT;

        // 调用助手类
        builderAssistant.addMappedStatement(id,
                sqlSource,
                sqlCommandType,
                parameterTypeClass,
                resultMap,
                resultTypeClass,
                keyGenerator,
                keyProperty,
                langDriver);

        // 给id加上namespace前缀
        id = builderAssistant.applyCurrentNamespace(id, false);

        // 存放键值生成器配置
        MappedStatement keyStatement = configuration.getMappedStatement(id);

        configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
    }
}

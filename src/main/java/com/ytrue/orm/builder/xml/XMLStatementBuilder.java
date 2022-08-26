package com.ytrue.orm.builder.xml;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.builder.MapperBuilderAssistant;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Element;

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

        SqlSource sqlSource = langDriver.createSqlSource(configuration, element, parameterTypeClass);

        // 调用助手类
        builderAssistant.addMappedStatement(id,
                sqlSource,
                sqlCommandType,
                parameterTypeClass,
                resultMap,
                resultTypeClass,
                langDriver);
    }
}

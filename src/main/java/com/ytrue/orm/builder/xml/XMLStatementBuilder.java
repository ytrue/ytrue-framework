package com.ytrue.orm.builder.xml;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.scripting.xmltags.XMLLanguageDriver;
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
     * 当前的命名空间
     */
    private String currentNamespace;


    /**
     * 这是是 select ，update,delete, insert 这样的标签
     */
    private Element element;


    public XMLStatementBuilder(Configuration configuration, Element element, String currentNamespace) {
        super(configuration);
        this.element = element;
        this.currentNamespace = currentNamespace;
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


        // 获取命令类型(select|insert|update|delete)
        String nodeName = element.getName();
        // 获取 SQL 指令类型
        SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));

        // 获取默认语言驱动器
        Class<?> langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
        LanguageDriver langDriver = configuration.getLanguageRegistry().getDriver(langClass);
        SqlSource sqlSource = langDriver.createSqlSource(configuration, element, parameterTypeClass);

        MappedStatement mappedStatement = MappedStatement.builder()
                .configuration(configuration)
                .id(currentNamespace + "." + id)
                .sqlCommandType(sqlCommandType)
                .sqlSource(sqlSource)
                .lang(langDriver)
                .resultType(resultTypeClass).build();

        // 添加解析 SQL
        configuration.addMappedStatement(mappedStatement);

    }
}

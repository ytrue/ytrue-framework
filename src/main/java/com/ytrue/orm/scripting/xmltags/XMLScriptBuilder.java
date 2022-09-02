package com.ytrue.orm.scripting.xmltags;

import com.ytrue.orm.builder.BaseBuilder;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.defaults.RawSqlSource;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();

    public XMLScriptBuilder(Configuration configuration, Element element, Class<?> parameterType) {
        super(configuration);
        this.element = element;
        this.parameterType = parameterType;
        initNodeHandlerMap();
    }

    private void initNodeHandlerMap() {
        // 9种，实现其中2种 trim/where/set/foreach/if/choose/when/otherwise/bind
        nodeHandlerMap.put("trim", new TrimHandler());
        nodeHandlerMap.put("if", new IfHandler());
    }

    public SqlSource parseScriptNode() {
        // 解析动态标签
        List<SqlNode> contents = parseDynamicTags(element);
        // 变成混合
        MixedSqlNode rootSqlNode = new MixedSqlNode(contents);

        SqlSource sqlSource;
        // 是否是动态的，是动态就给动态处理，否则静态处理
        if (isDynamic) {
            sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
        } else {
            sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
        }
        return sqlSource;
    }


    /**
     * 解析动态标签
     *
     * @param element
     * @return
     */
    List<SqlNode> parseDynamicTags(Element element) {
        List<SqlNode> contents = new ArrayList<>();
        List<Node> children = element.content();
        for (Node child : children) {
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String data = child.getText();
                TextSqlNode textSqlNode = new TextSqlNode(data);
                // 是否是动态的
                if (textSqlNode.isDynamic()) {
                    contents.add(textSqlNode);
                    isDynamic = true;
                } else {
                    contents.add(new StaticTextSqlNode(data));
                }
                // 如果当前节点是元素节点的话
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getName();
                NodeHandler handler = nodeHandlerMap.get(nodeName);
                // TODO 这里要针对对selectKey做处理
                if (handler == null) {
                    throw new RuntimeException("Unknown element <" + nodeName + "> in SQL statement.");
                }
                // 获取trim if标签
                handler.handleNode(element.element(child.getName()), contents);
                isDynamic = true;
            }
        }
        return contents;
    }


    private interface NodeHandler {
        /**
         * 处理node
         *
         * @param nodeToHandle
         * @param targetContents
         */
        void handleNode(Element nodeToHandle, List<SqlNode> targetContents);
    }

    private class TrimHandler implements NodeHandler {
        @Override
        public void handleNode(Element nodeToHandle, List<SqlNode> targetContents) {
            // 重复调取
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);

            String prefix = nodeToHandle.attributeValue("prefix");
            String prefixOverrides = nodeToHandle.attributeValue("prefixOverrides");
            String suffix = nodeToHandle.attributeValue("suffix");
            String suffixOverrides = nodeToHandle.attributeValue("suffixOverrides");
            TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
            targetContents.add(trim);
        }
    }

    private class IfHandler implements NodeHandler {
        @Override
        public void handleNode(Element nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String test = nodeToHandle.attributeValue("test");
            IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
            targetContents.add(ifSqlNode);
        }
    }

}

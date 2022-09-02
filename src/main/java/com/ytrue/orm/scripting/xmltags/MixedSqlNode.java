package com.ytrue.orm.scripting.xmltags;

import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/23 10:58
 * @description MixedSqlNode
 */
public class MixedSqlNode implements SqlNode {

    /**
     * 组合模式，拥有一个SqlNode的List
     */
    private List<SqlNode> contents;

    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {
        // 依次调用list里每个元素的apply  方便调式不使用 contents.forEach(node -> node.apply(context))
        for (SqlNode node : contents) {
            node.apply(context);
        }

        return true;
    }
}

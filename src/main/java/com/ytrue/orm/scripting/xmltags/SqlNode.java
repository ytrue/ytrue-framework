package com.ytrue.orm.scripting.xmltags;

/**
 * @author ytrue
 * @date 2022/8/23 09:50
 * @description SQL 节点
 */
public interface SqlNode {

    /**
     * 申请
     *
     * @param context
     * @return
     */
    boolean apply(DynamicContext context);
}

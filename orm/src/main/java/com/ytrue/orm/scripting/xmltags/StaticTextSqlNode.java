package com.ytrue.orm.scripting.xmltags;

/**
 * @author ytrue
 * @date 2022/8/23 09:52
 * @description StaticTextSqlNode
 */
public class StaticTextSqlNode implements SqlNode {

    /**
     * <select> text在这里 </select>标签里的数据
     */
    private String text;

    public StaticTextSqlNode(String text) {
        this.text = text;
    }


    @Override
    public boolean apply(DynamicContext context) {
        //将文本加入context
        context.appendSql(text);
        return true;
    }

}

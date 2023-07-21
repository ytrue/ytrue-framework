package com.ytrue.orm.reflection.property;

import lombok.Getter;
import lombok.ToString;

import java.util.Iterator;

/**
 * @author ytrue
 * @date 2022/8/19 14:05
 * @description 属性分解标记   例子：班级[0].学生.成绩
 */
@Getter
@ToString
public class PropertyTokenizer implements Iterable<PropertyTokenizer>, Iterator<PropertyTokenizer> {


    /**
     * 班级
     */
    private String name;

    /**
     * 班级[0]
     */
    private String indexedName;

    /**
     * 0
     */
    private String index;

    /**
     * 学生.成绩
     */
    private String children;


    public PropertyTokenizer(String fullName) {
        //班级[0].学生.成绩

        // delim == 5
        int delim = fullName.indexOf('.');
        if (delim > -1) {
            // name == 班级[0]
            name = fullName.substring(0, delim);
            // children == 学生.成绩
            children = fullName.substring(delim + 1);
        } else {
            // 找不到.的话，取全部部分
            name = fullName;
            children = null;
        }
        indexedName = name;

        // 把中括号里的数字给解析出来
        delim = name.indexOf('[');
        if (delim > -1) {
            // 获取[0]里面的 0
            index = name.substring(delim + 1, name.length() - 1);
            name = name.substring(0, delim);
        }

        //PropertyTokenizer(name=班级, indexedName=班级[0], index=0, children=学生.成绩)
    }

    @Override
    public Iterator<PropertyTokenizer> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return children != null;
    }

    /**
     * 取得下一个,非常简单，直接再通过儿子来new另外一个实例
     *
     * @return
     */
    @Override
    public PropertyTokenizer next() {
        return new PropertyTokenizer(children);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
    }
}

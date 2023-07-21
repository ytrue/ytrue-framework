package com.ytrue.orm.reflection;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/19 16:17
 * @description MetaObjectTest
 */
public class MetaObjectTest {


    @Test
    public void test01() {

        HashMap<String, List<Integer>> stringHashMap = new HashMap<>();
        stringHashMap.put("name", Arrays.asList(1, 2, 3));
        stringHashMap.put("age", Arrays.asList(4,4,5));

        MetaObject metaObject = SystemMetaObject.forObject(stringHashMap);


        System.out.println(metaObject.getValue("name[0]"));

    }

}

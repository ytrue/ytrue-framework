package com.ytrue.orm.reflection;

import lombok.Data;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/22 11:24
 * @description MetaClassTest
 */
public class MetaClassTest {


    @Test
    public void test01() {
        // MetaClass

        Teacher teacher = new Teacher();
        List<Teacher.Student> list = new ArrayList<>();
        list.add(new Teacher.Student());
        teacher.setName("小傅哥");
        teacher.setStudents(list);

        MetaClass metaClass = MetaClass.forClass(teacher.getClass());

    }


    @Data
    static class Teacher {

        private String name;

        private double price;

        private List<Student> students;

        private Student student;

        @Data
        public static class Student {

            private String id;
        }
    }

}

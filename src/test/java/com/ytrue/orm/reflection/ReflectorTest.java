package com.ytrue.orm.reflection;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/19 21:02
 * @description ReflectorTest
 */
@Slf4j
public class ReflectorTest {


    @Test
    public void test_reflection() {
        Teacher teacher = new Teacher();
        List<Teacher.Student> list = new ArrayList<>();
        list.add(new Teacher.Student());
        teacher.setName("ytrue");
        teacher.setStudents(list);
        MetaObject metaObject = SystemMetaObject.forObject(teacher);

        log.info("getGetterNames：{}", JSON.toJSONString(metaObject.getGetterNames()));
        log.info("getSetterNames：{}", JSON.toJSONString(metaObject.getSetterNames()));
        log.info("name的get方法返回值：{}", JSON.toJSONString(metaObject.getGetterType("name")));
        log.info("students的set方法参数值：{}", JSON.toJSONString(metaObject.getGetterType("students")));
        log.info("name的hasGetter：{}", metaObject.hasGetter("name"));
        log.info("student.id（属性为对象）的hasGetter：{}", metaObject.hasGetter("student.id"));
        log.info("获取name的属性值：{}", metaObject.getValue("name"));
        // 重新设置属性值
        metaObject.setValue("name", "小白");
        log.info("设置name的属性值：{}", metaObject.getValue("name"));
        // 设置属性（集合）的元素值
        metaObject.setValue("students[0].id", "001");
        metaObject.getValue("student.id");
        log.info("获取students集合的第一个元素的属性值：{}", JSON.toJSONString(metaObject.getValue("students[0].id")));
        log.info("对象的序列化：{}", JSON.toJSONString(teacher));
    }

    static class Teacher {

        private String name;

        private double price;

        private List<Student> students;

        private Student student;

        public static class Student {

            private String id;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public List<Student> getStudents() {
            return students;
        }

        public void setStudents(List<Student> students) {
            this.students = students;
        }

        public Student getStudent() {
            return student;
        }

        public void setStudent(Student student) {
            this.student = student;
        }
    }
}

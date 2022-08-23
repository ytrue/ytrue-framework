package com.ytrue.orm.test;

import com.ytrue.orm.builder.xml.XMLConfigBuilder;
import com.ytrue.orm.io.Resources;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.SqlSession;
import com.ytrue.orm.session.SqlSessionFactory;
import com.ytrue.orm.session.SqlSessionFactoryBuilder;
import com.ytrue.orm.session.defaults.DefaultSqlSession;
import com.ytrue.orm.test.dao.IUserDao;
import com.ytrue.orm.test.po.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;

/**
 * @author ytrue
 * @date 2022/8/11 15:09
 * @description ApiTest
 */
@Slf4j
public class ApiTest {


    @Test
    public void test_SqlSessionFactory() throws IOException {
        // 1. 从SqlSessionFactory中获取SqlSession
        Reader reader = Resources.getResourceAsReader("ytrue-orm-config.xml");

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        // 2. 生成代理对象
//        IUserDao userDao = sqlSession.getMapper(IUserDao.class);
//
//
//        // 3. 测试验证
//        for (int i = 0; i < 50; i++) {
//            User user = userDao.queryUserInfoById(1L);
//            log.info("测试结果：{}", user.toString());
//        }
    }

//    @Test
//    public void test_selectOne() throws IOException {
//        // 解析 XML
//        Reader reader = Resources.getResourceAsReader("ytrue-orm-config.xml");
//        XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(reader);
//        Configuration configuration = xmlConfigBuilder.parse();
//
//        // 获取 DefaultSqlSession
//        SqlSession sqlSession = new DefaultSqlSession(configuration);
//
//        // 执行查询：默认是一个集合参数
//        Object[] req = {1L};
//        Object res = sqlSession.selectOne("com.ytrue.orm.test.dao.IUserDao.queryUserInfoById", req);
//
//        System.out.println(res);
//    }
}

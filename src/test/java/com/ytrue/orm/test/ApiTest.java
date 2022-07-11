package com.ytrue.orm.test;

import com.ytrue.orm.binding.MapperRegistry;
import com.ytrue.orm.session.SqlSession;
import com.ytrue.orm.session.SqlSessionFactory;
import com.ytrue.orm.session.defaults.DefaultSqlSessionFactory;
import com.ytrue.orm.test.dao.IUserDao;
import org.junit.Test;

/**
 * @author ytrue
 * @date 2022/7/11 11:10
 * @description ApiTest
 */
public class ApiTest {

    @Test
    public void test_MapperProxyFactory() {
        // 1. 注册 Mapper
        MapperRegistry registry = new MapperRegistry();
        registry.addMappers("com.ytrue.orm.test.dao");

        // 2. 从 SqlSession 工厂获取 Session
        SqlSessionFactory sqlSessionFactory = new DefaultSqlSessionFactory(registry);
        SqlSession sqlSession = sqlSessionFactory.openSession();

        // 3. 获取映射器对象
        IUserDao userDao = sqlSession.getMapper(IUserDao.class);

        // 4. 测试验证
        String res = userDao.queryUserName("10001");


        System.out.println(res);
    }


}

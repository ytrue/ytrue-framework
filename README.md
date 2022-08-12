# 1.简介

实现一个简单的mybatis

# 2.工程结构
```text
mybatis-step-02
└── src
    ├── main
    │   └── java
    │       └── com.ytrue.orm
    │           ├── binding
    │           │   ├── MapperProxy.java
    │           │   ├── MapperProxyFactory.java
    │           │   └── MapperRegistry.java
    │           └── session
    │               ├── defaults
    │               │   ├── DefaultSqlSession.java
    │               │   └── DefaultSqlSessionFactory.java
    │               ├── SqlSession.java
    │               └── SqlSessionFactory.java
    └── test
        └── java
            └── com.ytrue.orm.test.dao
                ├── dao
                │   ├── ISchoolDao.java
                │   └── IUserDao.java
                └── ApiTest.java

```

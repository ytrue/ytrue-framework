# 1.简介

实现一个简单的mybatis

# 2.工程结构
```text
ytrue-orm
└── src
    ├── main
    │   └── java
    │       └── com.ytrue.orm
    │           ├── binding
    │           ├── builder
    │           ├── datasource
    │           │   ├── druid
    │           │   │   └── DruidDataSourceFactory.java
    │           │   ├── pooled
    │           │   │   ├── PooledConnection.java
    │           │   │   ├── PooledDataSource.java
    │           │   │   ├── PooledDataSourceFactory.java
    │           │   │   └── PoolState.java
    │           │   ├── unpooled
    │           │   │   ├── UnpooledDataSource.java
    │           │   │   └── UnpooledDataSourceFactory.java
    │           │   └── DataSourceFactory.java
    │           ├── executor
    │           ├── io
    │           ├── mapping
    │           ├── reflection
    │           │   ├── factory
    │           │   │   ├── DefaultObjectFactory.java
    │           │   │   └── ObjectFactory.java
    │           │   ├── invoker
    │           │   │   ├── GetFieldInvoker.java
    │           │   │   ├── Invoker.java
    │           │   │   ├── MethodInvoker.java
    │           │   │   └── SetFieldInvoker.java
    │           │   ├── property
    │           │   │   ├── PropertyNamer.java
    │           │   │   └── PropertyTokenizer.java
    │           │   ├── wrapper
    │           │   │   ├── BaseWrapper.java
    │           │   │   ├── BeanWrapper.java
    │           │   │   ├── CollectionWrapper.java
    │           │   │   ├── DefaultObjectWrapperFactory.java
    │           │   │   ├── MapWrapper.java
    │           │   │   ├── ObjectWrapper.java
    │           │   │   └── ObjectWrapperFactory.java
    │           │   ├── MetaClass.java
    │           │   ├── MetaObject.java
    │           │   ├── Reflector.java
    │           │   └── SystemMetaObject.java
    │           ├── session
    │           ├── transaction
    │           └── type
    └── test
        ├── java
        │   └── com.ytrue.orm.test.dao
        │       ├── dao
        │       │   └── IUserDao.java
        │       ├── po
        │       │   └── User.java
        │       ├── ApiTest.java
        │       └── ReflectionTest.java
        └── resources
            ├── mapper
            │   └──User_Mapper.xml
            └── mybatis-config-datasource.xml


```

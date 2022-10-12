# 1.简介

实现一个简单的mybatis

# 2.工程结构
```text
ytrue-orm
└── src
    ├─main
    │  ├─java
    │  │  └─com
    │  │      └─ytrue
    │  │          └─orm
    │  │              ├─annotations
    │  │              │      Delete.java
    │  │              │      Insert.java
    │  │              │      Select.java
    │  │              │      Update.java
    │  │              │      
    │  │              ├─binding
    │  │              │      MapperMethod.java
    │  │              │      MapperProxy.java
    │  │              │      MapperProxyFactory.java
    │  │              │      MapperRegistry.java
    │  │              │      
    │  │              ├─builder
    │  │              │  │  BaseBuilder.java
    │  │              │  │  MapperBuilderAssistant.java
    │  │              │  │  ParameterExpression.java
    │  │              │  │  ResultMapResolver.java
    │  │              │  │  SqlSourceBuilder.java
    │  │              │  │  StaticSqlSource.java
    │  │              │  │  
    │  │              │  ├─annotation
    │  │              │  │      MapperAnnotationBuilder.java
    │  │              │  │      
    │  │              │  └─xml
    │  │              │          XMLConfigBuilder.java
    │  │              │          XMLMapperBuilder.java
    │  │              │          XMLStatementBuilder.java
    │  │              │          
    │  │              ├─cache
    │  │              │  │  Cache.java
    │  │              │  │  CacheKey.java
    │  │              │  │  NullCacheKey.java
    │  │              │  │  TransactionalCacheManager.java
    │  │              │  │  
    │  │              │  ├─decorators
    │  │              │  │      FifoCache.java
    │  │              │  │      TransactionalCache.java
    │  │              │  │      
    │  │              │  └─impl
    │  │              │          PerpetualCache.java
    │  │              │          
    │  │              ├─datasource
    │  │              │  │  DataSourceFactory.java
    │  │              │  │  
    │  │              │  ├─druid
    │  │              │  │      DruidDataSourceFactory.java
    │  │              │  │      
    │  │              │  ├─pooled
    │  │              │  │      PooledConnection.java
    │  │              │  │      PooledDataSource.java
    │  │              │  │      PooledDataSourceFactory.java
    │  │              │  │      PoolState.java
    │  │              │  │      
    │  │              │  └─unpooled
    │  │              │          UnpooledDataSource.java
    │  │              │          UnpooledDataSourceFactory.java
    │  │              │          
    │  │              ├─executor
    │  │              │  │  BaseExecutor.java
    │  │              │  │  CachingExecutor.java
    │  │              │  │  ExecutionPlaceholder.java
    │  │              │  │  Executor.java
    │  │              │  │  SimpleExecutor.java
    │  │              │  │  
    │  │              │  ├─keygen
    │  │              │  │      Jdbc3KeyGenerator.java
    │  │              │  │      KeyGenerator.java
    │  │              │  │      NoKeyGenerator.java
    │  │              │  │      SelectKeyGenerator.java
    │  │              │  │      
    │  │              │  ├─parameter
    │  │              │  │      ParameterHandler.java
    │  │              │  │      
    │  │              │  ├─result
    │  │              │  │      DefaultResultContext.java
    │  │              │  │      DefaultResultHandler.java
    │  │              │  │      
    │  │              │  ├─resultset
    │  │              │  │      DefaultResultSetHandler.java
    │  │              │  │      ResultSetHandler.java
    │  │              │  │      ResultSetWrapper.java
    │  │              │  │      
    │  │              │  └─statement
    │  │              │          BaseStatementHandler.java
    │  │              │          PreparedStatementHandler.java
    │  │              │          SimpleStatementHandler.java
    │  │              │          StatementHandler.java
    │  │              │          
    │  │              ├─io
    │  │              │      Resources.java
    │  │              │      
    │  │              ├─mapping
    │  │              │      BoundSql.java
    │  │              │      CacheBuilder.java
    │  │              │      Environment.java
    │  │              │      MappedStatement.java
    │  │              │      ParameterMapping.java
    │  │              │      ResultFlag.java
    │  │              │      ResultMap.java
    │  │              │      ResultMapping.java
    │  │              │      SqlCommandType.java
    │  │              │      SqlSource.java
    │  │              │      
    │  │              ├─parsing
    │  │              │      GenericTokenParser.java
    │  │              │      TokenHandler.java
    │  │              │      
    │  │              ├─plugin
    │  │              │      Interceptor.java
    │  │              │      InterceptorChain.java
    │  │              │      Intercepts.java
    │  │              │      Invocation.java
    │  │              │      Plugin.java
    │  │              │      Signature.java
    │  │              │      
    │  │              ├─reflection
    │  │              │  │  MetaClass.java
    │  │              │  │  MetaObject.java
    │  │              │  │  Reflector.java
    │  │              │  │  SystemMetaObject.java
    │  │              │  │  
    │  │              │  ├─factory
    │  │              │  │      DefaultObjectFactory.java
    │  │              │  │      ObjectFactory.java
    │  │              │  │      
    │  │              │  ├─invoker
    │  │              │  │      GetFieldInvoker.java
    │  │              │  │      Invoker.java
    │  │              │  │      MethodInvoker.java
    │  │              │  │      SetFieldInvoker.java
    │  │              │  │      
    │  │              │  ├─property
    │  │              │  │      PropertyNamer.java
    │  │              │  │      PropertyTokenizer.java
    │  │              │  │      
    │  │              │  └─wrapper
    │  │              │          BaseWrapper.java
    │  │              │          BeanWrapper.java
    │  │              │          CollectionWrapper.java
    │  │              │          DefaultObjectWrapperFactory.java
    │  │              │          MapWrapper.java
    │  │              │          ObjectWrapper.java
    │  │              │          ObjectWrapperFactory.java
    │  │              │          
    │  │              ├─scripting
    │  │              │  │  LanguageDriver.java
    │  │              │  │  LanguageDriverRegistry.java
    │  │              │  │  
    │  │              │  ├─defaults
    │  │              │  │      DefaultParameterHandler.java
    │  │              │  │      RawSqlSource.java
    │  │              │  │      
    │  │              │  └─xmltags
    │  │              │          DynamicContext.java
    │  │              │          DynamicSqlSource.java
    │  │              │          ExpressionEvaluator.java
    │  │              │          IfSqlNode.java
    │  │              │          MixedSqlNode.java
    │  │              │          OgnlCache.java
    │  │              │          OgnlClassResolver.java
    │  │              │          SqlNode.java
    │  │              │          StaticTextSqlNode.java
    │  │              │          TextSqlNode.java
    │  │              │          TrimSqlNode.java
    │  │              │          XMLLanguageDriver.java
    │  │              │          XMLScriptBuilder.java
    │  │              │          
    │  │              ├─session
    │  │              │  │  Configuration.java
    │  │              │  │  LocalCacheScope.java
    │  │              │  │  ResultContext.java
    │  │              │  │  ResultHandler.java
    │  │              │  │  RowBounds.java
    │  │              │  │  SqlSession.java
    │  │              │  │  SqlSessionFactory.java
    │  │              │  │  SqlSessionFactoryBuilder.java
    │  │              │  │  TransactionIsolationLevel.java
    │  │              │  │  
    │  │              │  └─defaults
    │  │              │          DefaultSqlSession.java
    │  │              │          DefaultSqlSessionFactory.java
    │  │              │          
    │  │              ├─transaction
    │  │              │  │  Transaction.java
    │  │              │  │  TransactionFactory.java
    │  │              │  │  
    │  │              │  └─jdbc
    │  │              │          JdbcTransaction.java
    │  │              │          JdbcTransactionFactory.java
    │  │              │          
    │  │              └─type
    │  │                      BaseTypeHandler.java
    │  │                      DateTypeHandler.java
    │  │                      HashMapTypeHandler.java
    │  │                      JdbcType.java
    │  │                      LongTypeHandler.java
    │  │                      ObjectTypeHandler.java
    │  │                      SimpleTypeRegistry.java
    │  │                      StringTypeHandler.java
    │  │                      TypeAliasRegistry.java
    │  │                      TypeHandler.java
    │  │                      TypeHandlerRegistry.java
    │  │                      
    │  └─resources
    └─test
        ├─java
        │  └─com
        │      └─ytrue
        │          └─orm
        │              ├─datasource
        │              │  └─unpooled
        │              │          UnpooledDataSourceTest.java
        │              │          
        │              ├─reflection
        │              │  │  MetaClassTest.java
        │              │  │  MetaObjectTest.java
        │              │  │  ReflectorTest.java
        │              │  │  
        │              │  └─property
        │              │          PropertyNamerTest.java
        │              │          PropertyTokenizerTest.java
        │              │          
        │              └─test
        │                  │  ApiTest.java
        │                  │  ApiTest1.java
        │                  │  
        │                  ├─dao
        │                  │      IActivityDao.java
        │                  │      IUserDao.java
        │                  │      
        │                  ├─plugin
        │                  │      TestPlugin.java
        │                  │      
        │                  └─po
        │                          Activity.java
        │                          User.java
        │                          
        └─resources
            │  ytrue-orm-config.xml
            │  
            └─mapper
                    Activity_Mapper.xml
                    User_Mapper.xml


```

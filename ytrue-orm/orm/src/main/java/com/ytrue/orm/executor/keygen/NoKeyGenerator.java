package com.ytrue.orm.executor.keygen;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.MappedStatement;

import java.sql.Statement;

/**
 * @author ytrue
 * @date 2022/9/1 14:18
 * @description 默认空实现不对主键单独处理
 */
public class NoKeyGenerator implements KeyGenerator {
    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }
}

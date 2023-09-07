package com.ytrue.gateway.core.executor;

import com.ytrue.gateway.core.datasource.Connection;
import com.ytrue.gateway.core.executor.result.SessionResult;
import com.ytrue.gateway.core.mapping.HttpStatement;
import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.type.SimpleTypeRegistry;
import com.ytrue.gateway.core.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-07 10:50
 * @description 执行器抽象基类
 */
public abstract class BaseExecutor implements Executor {

    private final Logger logger = LoggerFactory.getLogger(BaseExecutor.class);

    protected Configuration configuration;

    protected Connection connection;

    public BaseExecutor(Configuration configuration, Connection connection) {
        this.configuration = configuration;
        this.connection = connection;
    }


    @Override
    public SessionResult exec(HttpStatement httpStatement, Map<String, Object> params) throws Exception {
        // 参数处理；后续的一些参数校验也可以在这里封装。
        String methodName = httpStatement.getMethodName();
        // 获取参数类型
        String parameterType = httpStatement.getParameterType();
        // 封装成数组
        String[] parameterTypes = new String[]{parameterType};
        // 请求参数，封装成数组
        Object[] args = SimpleTypeRegistry.isSimpleType(parameterType) ? params.values().toArray() : new Object[]{params};

        logger.info("执行器调用 method：{}#{}.{}({}) args：{}",
                httpStatement.getApplication(),
                httpStatement.getInterfaceName(),
                httpStatement.getMethodName(),
                GsonUtil.toJsonString(parameterTypes),
                GsonUtil.toJsonString(args)
        );
        // 抽象方法
        try {
            Object data = doExec(methodName, parameterTypes, args);
            logger.info("执行器调用结果:{}", data);
            return SessionResult.buildSuccess(data);
        } catch (Exception e) {
            return SessionResult.buildError(e.getMessage());
        }
    }


    /**
     * 执行操作
     *
     * @param methodName
     * @param parameterTypes
     * @param args
     * @return
     */
    protected abstract Object doExec(String methodName, String[] parameterTypes, Object[] args);
}

package com.ytrue.orm.parsing;

/**
 * @author ytrue
 * @date 2022/8/23 14:30
 * @description 记号处理器
 */
public interface TokenHandler {


    /**
     * 处理
     *
     * @param content
     * @return
     */
    String handleToken(String content);

}

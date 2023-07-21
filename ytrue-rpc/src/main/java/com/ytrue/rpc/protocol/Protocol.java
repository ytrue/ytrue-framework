package com.ytrue.rpc.protocol;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-05-19 14:28
 * @description Protocol
 */
public interface Protocol extends Serializable {

    /**
     * 幻术
     */
    String MAGIC_NUM = "ytrue-rpc";

    /**
     * 版本
     */
    byte PROTOCOL_VERSION = 1;
}

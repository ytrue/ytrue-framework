package com.ytrue.rpc.register;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author ytrue
 * @date 2023-05-19 14:55
 * @description HostAndPort
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HostAndPort {
    /**
     * 地址
     */
    private String hostName;

    /**
     * 端口号
     */
    private int port;
}

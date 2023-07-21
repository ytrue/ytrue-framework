package com.ytrue.rpc.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * @author ytrue
 * @date 2023-05-19 16:41
 * @description NetUtil
 */
public class NetUtil {

    private static final String LOCALHOST = "localhost";

    /**
     * 端口是否被使用
     *
     * @param port
     * @return
     */
    public static boolean isPortUsing(int port) {
        boolean flag = false;
        try {
            Socket socket = new Socket(LOCALHOST, port);
            socket.close();
            flag = true;
        } catch (IOException ignored) {

        }
        return flag;
    }

    /**
     * 获取地址
     *
     * @return
     * @throws UnknownHostException
     */
    public static String getHost() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * 根据输入端口号，递增递归查询可使用端口
     *
     * @param port 端口号
     * @return 如果被占用，递归；否则返回可使用port
     */
    public static int getUsablePort(int port) {

        if (NetUtil.isPortUsing(port)) {
            //端口被占用，port + 1递归
            port = port + 1;
            return getUsablePort(port);
        } else {
            //可用端口
            return port;
        }
    }
}

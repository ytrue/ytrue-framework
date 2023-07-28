package com.ytrue.netty.channel.socket.nio;

import com.ytrue.netty.channel.ChannelOption;

import java.io.IOException;
import java.net.SocketOption;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-07-28 10:15
 * @description NioChannelOption
 */
public class NioChannelOption<T> extends ChannelOption<T> {

    private final java.net.SocketOption<T> option;

    @SuppressWarnings("deprecation")
    private NioChannelOption(java.net.SocketOption<T> option) {
        super(option.name());
        this.option = option;
    }

    /**
     * 构建NioChannelOption
     *
     * @param option
     * @param <T>
     * @return
     */
    public static <T> ChannelOption<T> of(java.net.SocketOption<T> option) {
        return new NioChannelOption<T>(option);
    }

    /**
     * 给java的 Channel 设置参数
     *
     * @param jdkChannel
     * @param option
     * @param value
     * @param <T>
     * @return
     */
    static <T> boolean setOption(Channel jdkChannel, NioChannelOption<T> option, T value) {
        // NetworkChannel是Java NIO中的一个接口，它表示一个可以进行网络通信的通道。
        // NetworkChannel接口是Java NIO中Channel接口的一个子接口，
        // 它提供了一些额外的网络相关的方法，如获取通道的本地地址和远程地址、设置通道的阻塞模式等。
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;

        // NetworkChannel接口提供了一个方法supportedOptions()，该方法返回一个Set集合，包含当前网络通道所支持的所有选项。
        if (!channel.supportedOptions().contains(option.option)) {
            return false;
        }

        // 在Java中，StandardSocketOptions.IP_TOS是一个枚举常量，它代表了一个网络套接字选项，用于设置IP包的服务类型（Type of Service，TOS）字段。
        // IP_TOS选项允许你设置发送的IP包的服务类型，以便网络设备可以根据不同的服务类型对包进行不同的处理。服务类型字段通常用于指定包的优先级、延迟、吞吐量等。
        // 具体来说，StandardSocketOptions.IP_TOS的意思是：
        //  - IP_TOS表示一个用于设置IP包服务类型字段的套接字选项。
        //  - 通过设置IP_TOS选项，你可以设置发送的IP包的服务类型，以便网络设备可以根据服务类型对包进行不同的处理。
        //  - IP_TOS选项的值通常是一个8位二进制数，表示不同的服务类型。
        //  - 在Java中，可以使用SocketOption类的静态字段IP_TOS来访问该选项。
        if (channel instanceof ServerSocketChannel && option.option == java.net.StandardSocketOptions.IP_TOS) {
            return false;
        }

        try {
            // 设置参数
            channel.setOption(option.option, value);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取参数
     *
     * @param jdkChannel
     * @param option
     * @param <T>
     * @return
     */
    static <T> T getOption(Channel jdkChannel, NioChannelOption<T> option) {
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;

        if (!channel.supportedOptions().contains(option.option)) {
            return null;
        }
        if (channel instanceof ServerSocketChannel && option.option == java.net.StandardSocketOptions.IP_TOS) {
            return null;
        }
        try {
            return channel.getOption(option.option);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static ChannelOption[] getOptions(Channel jdkChannel) {
        // 强制转换获取 NetworkChannel
        java.nio.channels.NetworkChannel channel = (java.nio.channels.NetworkChannel) jdkChannel;
        // 获取NetworkChannel支持的参数
        Set<SocketOption<?>> supportedOpts = channel.supportedOptions();

        // 如何是 ServerSocketChannel
        if (channel instanceof ServerSocketChannel) {
            // 创建一个list集合
            List<ChannelOption<?>> extraOpts = new ArrayList<>(supportedOpts.size());
            // 循环处理
            for (java.net.SocketOption<?> opt : supportedOpts) {
                // 如果opt == java.net.StandardSocketOptions.IP_TOS 就跳过当前追加
                if (opt == java.net.StandardSocketOptions.IP_TOS) {
                    continue;
                }
                // 加入集合中，包装成NioChannelOption
                extraOpts.add(new NioChannelOption(opt));
            }
            // 返回数组
            return extraOpts.toArray(new ChannelOption[0]);
        } else {
            // 创建数组大小
            ChannelOption<?>[] extraOpts = new ChannelOption[supportedOpts.size()];

            int i = 0;
            for (java.net.SocketOption<?> opt : supportedOpts) {
                // 包装一下，并且插入
                extraOpts[i++] = new NioChannelOption(opt);
            }
            return extraOpts;
        }
    }
}

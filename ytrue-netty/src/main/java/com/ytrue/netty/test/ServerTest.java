package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.ServerBootstrap;
import com.ytrue.netty.channel.ChannelFuture;
import com.ytrue.netty.channel.ChannelOption;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.nio.NioServerSocketChannel;
import com.ytrue.netty.util.AttributeKey;

import java.io.IOException;

public class ServerTest {

    public static AttributeKey<Integer> INDEX_KEY = AttributeKey.valueOf("常量");

    public static void main(String[] args) throws IOException, InterruptedException {

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);

        ChannelFuture channelFuture = serverBootstrap.group(bossGroup,workerGroup).
                channel(NioServerSocketChannel.class).
                //这里我们把服务端接受连接的数量设置为1，超过这个连接数应该就会报错。
                //是这个错误：java.net.SocketException: Connection reset by peer，
                //服务器接受的客户端连接超过了其设定最大值，就会关闭一些已经已经接受成功的连接
                //这里参数不能设置为0，在源码中会对option()的value进行判断：backlog < 1 ? 50 : backlog，传入的参数小于1就会使用默认配置50
                //这节课我们暂时先不演示attr方法，下节课会引入
                //ChannelPipeline，我们会结合ChannelPipeline演示attr方法，让大家看到用户存入channel的数据是怎么在ChannelPipeline
                //的每一个channelHandler中得到了
                option(ChannelOption.SO_BACKLOG,1).
                //attr(INDEX_KEY,10).
                bind(9993).addListener(future -> System.out.println("我绑定成功了")).sync();
    }
}

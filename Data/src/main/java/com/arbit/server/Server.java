package com.arbit.server;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class Server extends Thread{
    private ConcurrentHashMap<String, Object> concurrentMap;

    public Server(ConcurrentHashMap<String, Object> concurrentMap) {
        this.concurrentMap = concurrentMap;
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(ch.qos.logback.classic.Level.ERROR);
    }

    public void run() {
        try {
            NioEventLoopGroup bossGroup = new NioEventLoopGroup();
            NioEventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                    ServerBootstrap serverBootstrap = new ServerBootstrap()
                        .group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();

                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                pipeline.addLast(new WebSocketFrameAggregator(65536));
                                pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

                                pipeline.addLast(new WebSocketHadler(concurrentMap));

                            
                            }
                        });

                    ChannelFuture future = serverBootstrap.bind(8080).sync();
                    future.channel().closeFuture().sync();
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
        
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
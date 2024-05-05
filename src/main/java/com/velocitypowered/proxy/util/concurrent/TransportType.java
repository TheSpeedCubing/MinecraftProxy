package com.velocitypowered.proxy.util.concurrent;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;

public class TransportType {
    public static TransportType NIO = new TransportType("NIO", NioServerSocketChannel::new,
            NioSocketChannel::new,
            NioDatagramChannel::new,
            (name, type) -> new NioEventLoopGroup(0, createThreadFactory(name, type)));
    public static TransportType EPOLL = new TransportType("epoll", EpollServerSocketChannel::new,
            EpollSocketChannel::new,
            EpollDatagramChannel::new,
            (name, type) -> new EpollEventLoopGroup(0, createThreadFactory(name, type)));
    public static TransportType KQUEUE = new TransportType("kqueue", KQueueServerSocketChannel::new,
            KQueueSocketChannel::new,
            KQueueDatagramChannel::new,
            (name, type) -> new KQueueEventLoopGroup(0, createThreadFactory(name, type)));
    public final String name;
    public final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory;
    public final ChannelFactory<? extends SocketChannel> socketChannelFactory;
    public final ChannelFactory<? extends DatagramChannel> datagramChannelFactory;
    public final BiFunction<String, String, EventLoopGroup> eventLoopGroupFactory;

    public TransportType(final String name,
                         final ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory,
                         final ChannelFactory<? extends SocketChannel> socketChannelFactory,
                         final ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
                         final BiFunction<String, String, EventLoopGroup> eventLoopGroupFactory) {
        this.name = name;
        this.serverSocketChannelFactory = serverSocketChannelFactory;
        this.socketChannelFactory = socketChannelFactory;
        this.datagramChannelFactory = datagramChannelFactory;
        this.eventLoopGroupFactory = eventLoopGroupFactory;
    }

    public static TransportType bestType(boolean disableNativeTransport) {
        if (disableNativeTransport) {
            return NIO;
        }

        if (Epoll.isAvailable()) {
            return EPOLL;
        }

        if (KQueue.isAvailable()) {
            return KQUEUE;
        }

        return NIO;
    }

    public EventLoopGroup createEventLoopGroup(final String type) {
        return this.eventLoopGroupFactory.apply(this.name, type);
    }

    private static ThreadFactory createThreadFactory(final String name, final String type) {
        return new VelocityNettyThreadFactory("Netty " + name + ' ' + type + " #%d");
    }
}

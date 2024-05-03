package top.speedcubing.minecraftproxy.server;

import com.google.gson.JsonObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.minecraftproxy.handler.ConnectionHandler;

public class Node {
    public ChannelFuture future;
    public final String name;
    public final int localport;
    public final List<BackendServer> servers;
    public final Set<CIDR> blockedCIDR;

    public final boolean statusRequest; //allow status request, false -> disconnect
    public final boolean loginRequest; //allow login request, false -> disconnect
    public final boolean loginRequestTimeout; //if loginRequest = false, false -> timeout
    public final boolean kick;
    public final String kickMessage;

    public final EventLoopGroup bossGroup = new NioEventLoopGroup();
    public final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public Node(JsonObject o) {

        List<BackendServer> servers = new ArrayList<>();
        o.get("servers").getAsJsonArray().forEach(a -> servers.add(new BackendServer(a.getAsJsonObject().get("address").getAsString(), a.getAsJsonObject().get("proxy-protocol").getAsString())));

        Set<CIDR> blockedCIDR = new HashSet<>();
        o.get("blockedCIDR").getAsJsonArray().forEach(a -> blockedCIDR.add(new CIDR(a.getAsString())));

        this.name = o.get("name").getAsString();
        this.localport = o.get("localport").getAsInt();
        this.servers = servers;
        this.blockedCIDR = blockedCIDR;

        this.statusRequest = o.get("statusRequest").getAsBoolean();
        this.loginRequest = o.get("loginRequest").getAsBoolean();
        this.loginRequestTimeout = o.get("loginRequestTimeout").getAsBoolean();

        this.kick = o.get("kick").getAsBoolean();
        this.kickMessage = o.get("kickMessage").getAsString();
    }

    public void createBootstrap() throws InterruptedException {
        this.future = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1 << 20, 1 << 21))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x18)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ConnectionHandler(Node.this));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true).bind(this.localport).sync();
    }

    public String toString() {
        return name;
    }
}

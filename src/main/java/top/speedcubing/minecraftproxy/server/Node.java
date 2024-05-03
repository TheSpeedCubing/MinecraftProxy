package top.speedcubing.minecraftproxy.server;

import com.google.gson.JsonObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.minecraftproxy.Main;
import top.speedcubing.minecraftproxy.handler.ServerChannelInitializer;

public class Node {
    public final String name;

    public final InetSocketAddress address;
    public final boolean tcpFastOpen;

    public final int readTimeout;

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

        this.address = new InetSocketAddress(o.get("address").getAsString(), o.get("port").getAsInt());
        this.tcpFastOpen = o.get("tcpFastOpen").getAsBoolean();
        this.readTimeout = o.get("readTimeout").getAsInt();

        this.servers = servers;
        this.blockedCIDR = blockedCIDR;

        this.statusRequest = o.get("statusRequest").getAsBoolean();
        this.loginRequest = o.get("loginRequest").getAsBoolean();
        this.loginRequestTimeout = o.get("loginRequestTimeout").getAsBoolean();

        this.kick = o.get("kick").getAsBoolean();
        this.kickMessage = o.get("kickMessage").getAsString();
    }

    public void createBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1 << 20, 1 << 21))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x18)
                .childHandler(new ServerChannelInitializer(this))
                .localAddress(address);

        if (tcpFastOpen)
            bootstrap.option(ChannelOption.TCP_FASTOPEN, 3);

        bootstrap.bind().addListener((ChannelFutureListener) future -> {
            final Channel channel = future.channel();
            if (future.isSuccess()) {
                Main.print("Listening on " + channel.localAddress());
            } else {
                Main.print("Can't bind to " + address + "\n" + future.cause());
            }
        });
    }

    public String toString() {
        return name;
    }
}

package top.speedcubing.mcproxy.server;

import com.google.gson.JsonObject;
import com.velocitypowered.proxy.util.concurrent.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.handler.ClientInitializer;

public class Node {
    public final String name;
    private final InetSocketAddress address;
    private final boolean tcpFastOpen;
    public final int readTimeout;
    public final boolean log;
    public final List<BackendServer> servers;
    public final Set<CIDR> blockedCIDR;

    private final TransportType transportType;
    public final EventLoopGroup bossGroup;
    public final EventLoopGroup workerGroup;

    public Node(JsonObject o) {

        List<BackendServer> servers = new ArrayList<>();
        o.get("servers").getAsJsonArray().forEach(a -> servers.add(new BackendServer(a.getAsJsonObject().get("address").getAsString(), a.getAsJsonObject().get("proxy-protocol").getAsString())));

        Set<CIDR> blockedCIDR = new HashSet<>();
        o.get("blockedCIDR").getAsJsonArray().forEach(a -> blockedCIDR.add(new CIDR(a.getAsString())));

        this.name = o.get("name").getAsString();

        this.log = o.get("log").getAsBoolean();

        this.address = new InetSocketAddress(o.get("address").getAsString(), o.get("port").getAsInt());
        this.tcpFastOpen = o.get("tcpFastOpen").getAsBoolean();
        this.readTimeout = o.get("readTimeout").getAsInt();

        this.servers = servers;
        this.blockedCIDR = blockedCIDR;

        this.transportType = TransportType.bestType(o.get("disableNativeTransport").getAsBoolean());
        this.bossGroup = this.transportType.createEventLoopGroup("Boss");
        this.workerGroup = this.transportType.createEventLoopGroup("Worker");
    }

    public void createBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(this.transportType.serverSocketChannelFactory)
                .group(this.bossGroup, this.workerGroup)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1 << 20, 1 << 21))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x18)
                .childHandler(new ClientInitializer(this))
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

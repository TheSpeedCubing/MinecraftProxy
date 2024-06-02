package top.speedcubing.mcproxy.server;

import com.velocitypowered.proxy.util.concurrent.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
    private final InetSocketAddress address;
    private NodeSetting nodeSetting;

    public Set<CIDR> blockedCIDR;
    public List<BackendServer> servers;

    public TransportType transportType;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ChannelFuture future;

    public Node(InetSocketAddress address, NodeSetting nodeSetting) {
        this.address = address;

        loadSettings(nodeSetting);

        this.transportType = TransportType.bestType(getSetting("disableNativeTransport").getAsBoolean());
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Setting getSetting(String name) {
        return nodeSetting.getSettings().get(name);
    }

    public boolean loadSettings(NodeSetting other) {
        if (nodeSetting != null && nodeSetting.requireRestart(other)) {
            return true;
        }
        this.nodeSetting = other;

        Set<CIDR> blockedCIDR = new HashSet<>();
        getSetting("blockedCIDR").getAsJsonArray().forEach(a -> blockedCIDR.add(new CIDR(a.getAsString())));
        this.blockedCIDR = blockedCIDR;

        List<BackendServer> servers = new ArrayList<>();
        getSetting("servers").getAsJsonArray().forEach(a -> servers.add(new BackendServer(a.getAsJsonObject())));
        this.servers = servers;

        return false;
    }

    public void startup() {
        try {
            this.bossGroup = this.transportType.createEventLoopGroup("Boss");
            this.workerGroup = this.transportType.createEventLoopGroup("Worker");
            createBootstrap();
            Main.print("Created ServerBootstrap for " + this);
        } catch (Exception e) {
            shutdown();
            e.printStackTrace();
        }
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

        if (getSetting("tcpFastOpen").getAsBoolean())
            bootstrap.option(ChannelOption.TCP_FASTOPEN, 3);

        future = bootstrap.bind();

        future.addListener((ChannelFutureListener) future -> {
            final Channel channel = future.channel();
            if (future.isSuccess()) {
                Main.print("Listening on " + channel.localAddress());
            } else {
                Main.print("Can't bind to " + address + "\n" + future.cause());
            }
        });
    }

    public void shutdown() {
        if (future != null) {
            future.channel().close().addListener((ChannelFutureListener) future -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
        } else {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    @Override
    public String toString() {
        return "Node{name=" + getSetting("name").getAsString() + ",bind=" + address + "}";
    }
}

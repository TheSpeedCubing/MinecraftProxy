package top.speedcubing.minecraftproxy.netty;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import top.speedcubing.lib.utils.internet.ip.CIDR;

import java.util.*;

public class Node {
    public ChannelFuture future;
    public final String name;
    public final int localport;
    public final List<BackendServer> servers;
    public final Set<CIDR> blockedCIDR;
    public final EventLoopGroup bossGroup = new NioEventLoopGroup();
    public final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public Node(String name, int localport, List<BackendServer> servers, Set<CIDR> blockedCIDR) {
        this.name = name;
        this.localport = localport;
        this.servers = servers;
        this.blockedCIDR = blockedCIDR;
    }

    public String toString() {
        return name;
    }
}

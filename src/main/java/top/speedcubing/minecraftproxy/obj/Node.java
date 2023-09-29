package top.speedcubing.minecraftproxy.obj;

import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;

public class Node {
    public ChannelFuture future;
    public String name;
    public int localPort;
    public String remoteHost;
    public int remotePort;
    public HAProxyProtocolVersion HAProxy = null;
    public EventLoopGroup bossGroup = new NioEventLoopGroup();
    public EventLoopGroup workerGroup = new NioEventLoopGroup();

    public Node(String name,int localPort, String remoteAddress, String HAProxy) {
        this.name = name;
        this.localPort = localPort;
        String[] s = remoteAddress.split(":");
        this.remoteHost = s[0];
        this.remotePort = s.length == 1 ? 25565 : Integer.parseInt(s[1]);
        switch (HAProxy) {
            case "v1":
                this.HAProxy = HAProxyProtocolVersion.V1;
                break;
            case "v2":
                this.HAProxy = HAProxyProtocolVersion.V2;
                break;
        }
    }

    public String toString() {
        return name;
    }
}

package top.speedcubing.minecraftproxy.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import java.util.Random;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.minecraftproxy.netty.Node;

public class CIDRBlockingHandler extends ChannelInboundHandlerAdapter {
    private final Node node;

    public CIDRBlockingHandler(Node node) {
        this.node = node;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        for (CIDR cidr : node.blockedCIDR) {
            if (cidr.contains(playerAddress.getAddress().getHostAddress())) {
                ctx.close();
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}

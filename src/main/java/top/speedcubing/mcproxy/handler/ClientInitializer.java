package top.speedcubing.mcproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.server.Node;

public class ClientInitializer extends ChannelInitializer<Channel> {
    private final Node node;

    public ClientInitializer(Node node) {
        this.node = node;
    }

    @Override
    public void initChannel(Channel ch) {
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(node.readTimeout, TimeUnit.MILLISECONDS));
        ch.pipeline().addLast("client-handler", new ClientHandler(node));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Main.print("(1) ");
        cause.printStackTrace();
        ctx.close();
    }
}

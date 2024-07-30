package top.speedcubing.mcproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import top.speedcubing.mcproxy.server.Node;
import top.speedcubing.mcproxy.session.Session;

public class ClientInitializer extends ChannelInitializer<Channel> {
    private final Node node;
    private Session session;

    public ClientInitializer(Node node) {
        this.node = node;
    }

    @Override
    public void initChannel(Channel ch) {
        this.session = new Session(node);
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(session.node.getSetting("readTimeout").getAsInteger(), TimeUnit.MILLISECONDS));
        ch.pipeline().addLast("client-handler", new ClientHandler(session));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (session != null)
            session.close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (session != null)
            session.handleException(ctx, cause, "ClientInitializer");
    }
}

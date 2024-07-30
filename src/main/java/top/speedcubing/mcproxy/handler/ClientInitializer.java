package top.speedcubing.mcproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import top.speedcubing.mcproxy.session.Session;

public class ClientInitializer extends ChannelInitializer<Channel> {
    private final Session session;

    public ClientInitializer(Session session) {
        this.session = session;
    }

    @Override
    public void initChannel(Channel ch) {
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(session.node.getSetting("readTimeout").getAsInteger(), TimeUnit.MILLISECONDS));
        ch.pipeline().addLast("client-handler", new ClientHandler(session));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        session.close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        session.handleException(ctx, cause, "ClientInitializer");
    }
}

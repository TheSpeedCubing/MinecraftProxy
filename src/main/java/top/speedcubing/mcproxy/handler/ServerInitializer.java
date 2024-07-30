package top.speedcubing.mcproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import top.speedcubing.mcproxy.session.Session;

public class ServerInitializer extends ChannelInitializer<Channel> {
    private final Session session;

    ServerInitializer(Session session) {
        this.session = session;
    }

    @Override
    public void initChannel(Channel ch) {
        ch.pipeline().addLast("server-handler", new ServerHandler(session));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        session.close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        session.handleException(ctx, cause, "ServerInitializer");
    }
}

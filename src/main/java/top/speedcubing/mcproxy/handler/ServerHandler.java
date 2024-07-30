package top.speedcubing.mcproxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import top.speedcubing.mcproxy.session.Session;

public class ServerHandler extends ChannelInboundHandlerAdapter {


    private final Session session;

    ServerHandler(Session session) {
        this.session = session;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        session.clientChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        session.close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        session.handleException(ctx, cause, "ServerHandler");
    }
}

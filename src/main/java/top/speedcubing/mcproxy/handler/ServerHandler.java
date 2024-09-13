package top.speedcubing.mcproxy.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import top.speedcubing.mcproxy.config;
import top.speedcubing.mcproxy.event.PacketEvent;
import top.speedcubing.mcproxy.packet.clientbound.ClientboundPacket;
import top.speedcubing.mcproxy.session.Session;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private final Session session;

    ServerHandler(Session session) {
        this.session = session;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        session.serverHandler = ctx;
        session.serverChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() == 0) {
            ReferenceCountUtil.release(msg);
            return;
        }

        if (!config.readDetail) {
            session.clientChannel.writeAndFlush(msg);
            return;
        }

        if (session.encryptMode) {
            session.clientChannel.writeAndFlush(msg);
            return;
        }

        ClientboundPacket packet = ClientboundPacket.createClientBoundPacket(session, session.serverPacketLength, buf);
        PacketEvent.handle(session, ctx, packet);
        session.clientChannel.writeAndFlush(packet.encode());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        session.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        session.handleException(cause, "ServerHandler");
    }
}

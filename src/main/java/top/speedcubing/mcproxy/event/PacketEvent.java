package top.speedcubing.mcproxy.event;

import io.netty.channel.ChannelHandlerContext;
import top.speedcubing.mcproxy.packet.ClientboundPacket;
import top.speedcubing.mcproxy.packet.serverbound.SHandshakePacket;
import top.speedcubing.mcproxy.packet.serverbound.SLoginEncryptionResponsePacket;
import top.speedcubing.mcproxy.packet.ServerboundPacket;
import top.speedcubing.mcproxy.session.Session;

public class PacketEvent {
    public static State handle(Session session, ChannelHandlerContext ctx, Object packet) {
        try {
            handlePacket(session, ctx, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (session.handshakeProgress == State.HANDSHAKE) {
            int nextState = ((SHandshakePacket) packet).getNextState();

            if (nextState == 2) {
                return State.LOGIN;
            } else
                return State.STATUS;
        }
        if (session.handshakeProgress == State.LOGIN) {
            if (packet instanceof SLoginEncryptionResponsePacket)
                return State.PLAY;
        }
        return session.handshakeProgress;
    }

    public static void handlePacket(Session session, ChannelHandlerContext ctx, Object packet) throws Exception {
        if (packet instanceof ServerboundPacket)
            ServerBoundEvent.handleServerBoundPacket(session, ctx, (ServerboundPacket) packet);
        else
            ClientBoundEvent.handleClientBoundPacket(session, ctx, (ClientboundPacket) packet);
    }
}

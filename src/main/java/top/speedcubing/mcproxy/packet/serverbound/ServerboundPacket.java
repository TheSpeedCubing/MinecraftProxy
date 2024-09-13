package top.speedcubing.mcproxy.packet.serverbound;


import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.event.State;
import top.speedcubing.mcproxy.session.Session;
import top.speedcubing.mcproxy.packet.Packet;

public class ServerboundPacket extends Packet<ServerboundPacket> {

    public ServerboundPacket(ByteBuf buf) {
        super(buf);
    }

    public static ServerboundPacket createServerBoundPacket(Session session, int packetLength, ByteBuf buf) {
        int index = buf.readerIndex();
        int packetID = ByteBufUtils.readVarInt(buf);

        index = buf.readerIndex() - index;

        Packet<ServerboundPacket> p = null;
        if (session.handshakeProgress == State.HANDSHAKE) {
            if (packetID == 0x00) {
                p = new SHandshakePacket(buf);
            }
        }
        if (session.handshakeProgress == State.STATUS) {
            if (packetID == 0x00) {
                p = new SStatusRequestPacket(buf);
            }
            if (packetID == 0x01) {
                p = new SStatusPingRequestPacket(buf);
            }
        }
        if (session.handshakeProgress == State.LOGIN) {
            if (packetID == 0x00) {
                p = new SLoginStartPacket(buf);
            }
            if (packetID == 0x01) {
                p = new SLoginEncryptionResponsePacket(buf);
            }
        }

        if (p == null)
            p = new SUnknownPacket(buf, index);

        return (ServerboundPacket) p.assign(packetLength, packetID);
    }
}

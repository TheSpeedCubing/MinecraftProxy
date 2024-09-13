package top.speedcubing.mcproxy.packet.clientbound;


import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.event.State;
import top.speedcubing.mcproxy.packet.Packet;
import top.speedcubing.mcproxy.session.Session;

public class ClientboundPacket extends Packet<ClientboundPacket> {

    public ClientboundPacket(ByteBuf buf) {
        super(buf);
    }

    public static ClientboundPacket createClientBoundPacket(Session session, int packetLength, ByteBuf buf) {
        int index = buf.readerIndex();
        int packetID = ByteBufUtils.readVarInt(buf);

        index = buf.readerIndex() - index;

        Packet<ClientboundPacket> p = null;
        if (session.handshakeProgress == State.STATUS) {
            if (packetID == 0x00) {
                p = new CStatusResponsePacket(buf);
            }
            if (packetID == 0x01) {
                p = new CStatusPingResponsePacket(buf);
            }
        }
        if (session.handshakeProgress == State.LOGIN) {
            if (packetID == 0x00) {
                //disconnect
                p = new CUnknownPacket(buf, index);
            }
            if (packetID == 0x01) {
                p = new CLoginEncryptionRequestPacket(buf);
            }
        }
        if (p == null)
            p = new CUnknownPacket(buf, index);

        return (ClientboundPacket) p.assign(packetLength, packetID);
    }
}

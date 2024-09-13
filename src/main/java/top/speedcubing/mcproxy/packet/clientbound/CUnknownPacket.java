package top.speedcubing.mcproxy.packet.clientbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.mcproxy.packet.ClientboundPacket;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class CUnknownPacket extends ClientboundPacket {

    byte[] b;
    int index;

    public CUnknownPacket(ByteBuf buf, int index) {
        super(buf);
        System.out.println(index);
        this.index = index;
    }

    @Override
    public void decode(ByteBuf buf) {
        b = new byte[getPacketLength() - index];
        buf.readBytes(b);
        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        buf.writeBytes(b);
        return buf;
    }
}

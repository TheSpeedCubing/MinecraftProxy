package top.speedcubing.mcproxy.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;

public class Packet<T> {

    public void decode(ByteBuf buf) {

    }

    public ByteBuf encode() {
        return null;
    }

    private int packetLength;
    private int packetID;
    private final ByteBuf buf;

    public Packet<T> assign(int packetLength, int packetID) {
        this.packetLength = packetLength;
        this.packetID = packetID;
        decode(buf);
        return this;
    }

    public ByteBuf createBuf() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufUtils.writeVarInt(buf, getPacketLength());
        ByteBufUtils.writeVarInt(buf, packetID);
        return buf;
    }

    public Packet(ByteBuf buf) {
        this.buf = buf;
    }

    public int getPacketLength() {
        return packetLength;
    }
}

package top.speedcubing.mcproxy.packet.serverbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class SStatusPingRequestPacket extends ServerboundPacket {

    private long payload;

    public SStatusPingRequestPacket(ByteBuf buf) {
        super(buf);
    }

    public long getPayload() {
        return payload;
    }

    public void setPayload(long payload) {
        this.payload = payload;
    }

    @Override
    public void decode(ByteBuf buf) {
        this.payload = buf.readLong();

        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        buf.writeLong(payload);
        return buf;
    }
}

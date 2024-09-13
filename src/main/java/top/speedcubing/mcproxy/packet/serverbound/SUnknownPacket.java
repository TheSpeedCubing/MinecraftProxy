package top.speedcubing.mcproxy.packet.serverbound;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class SUnknownPacket extends ServerboundPacket {

    byte[] b;
    int index;

    public SUnknownPacket(ByteBuf buf, int index) {
        super(buf);
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

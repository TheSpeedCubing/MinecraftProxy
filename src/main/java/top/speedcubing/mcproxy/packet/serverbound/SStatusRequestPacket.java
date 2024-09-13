package top.speedcubing.mcproxy.packet.serverbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class SStatusRequestPacket extends ServerboundPacket {
    public SStatusRequestPacket(ByteBuf buf) {
        super(buf);
    }


    @Override
    public void decode(ByteBuf buf) {
        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        return createBuf();
    }
}

package top.speedcubing.mcproxy.packet.serverbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class SLoginStartPacket extends ServerboundPacket {

    private String name;

    public SLoginStartPacket(ByteBuf buf) {
        super(buf);
    }
    public String getName() {
        return name;
    }

    public void setPayload(String name) {
        this.name = name;
    }

    @Override
    public void decode(ByteBuf buf) {
        this.name = ByteBufUtils.readString(buf);

        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        ByteBufUtils.writeString(buf, this.name);
        return buf;
    }
}

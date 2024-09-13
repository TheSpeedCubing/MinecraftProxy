package top.speedcubing.mcproxy.packet.clientbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class CStatusResponsePacket extends ClientboundPacket {

    private String string;

    public CStatusResponsePacket(ByteBuf buf) {
        super(buf);
    }

    public String getJSONResponse() {
        return string;
    }

    public void setPayload(String string) {
        this.string = string;
    }

    @Override
    public void decode(ByteBuf buf) {
        this.string = ByteBufUtils.readString(buf);

        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        ByteBufUtils.writeString(buf, string);
        return buf;
    }
}

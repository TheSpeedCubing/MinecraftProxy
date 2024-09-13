package top.speedcubing.mcproxy.packet.serverbound;


import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class SHandshakePacket extends ServerboundPacket {
    private int protocolVersion;
    private String serverAddress;
    private int serverPort;
    private int nextState;

    public SHandshakePacket(ByteBuf buf) {
        super(buf);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getNextState() {
        return nextState;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setNextState(int nextState) {
        this.nextState = nextState;
    }

    @Override
    public void decode(ByteBuf buf) {

        this.protocolVersion = ByteBufUtils.readVarInt(buf);
        this.serverAddress = ByteBufUtils.readString(buf);
        this.serverPort = buf.readUnsignedShort();
        this.nextState = ByteBufUtils.readVarInt(buf);

        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        ByteBufUtils.writeVarInt(buf, protocolVersion);
        ByteBufUtils.writeString(buf, serverAddress);
        buf.writeShort(serverPort);
        ByteBufUtils.writeVarInt(buf, nextState);
        return buf;
    }

    @Override
    public String toString() {
        return "HandshakePacket[" + protocolVersion + "," + serverAddress + "," + serverPort + "," + nextState + "]";
    }
}

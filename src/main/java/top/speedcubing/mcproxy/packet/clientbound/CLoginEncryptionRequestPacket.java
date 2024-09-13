package top.speedcubing.mcproxy.packet.clientbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.packet.ClientboundPacket;
import top.speedcubing.mcproxy.packet.PacketUtils;

public class CLoginEncryptionRequestPacket extends ClientboundPacket {
    private String serverID;
    private int publicKeyLength;
    private byte[] publicKey;
    private int verifyTokenLength;
    private byte[] verifyToken;


    public CLoginEncryptionRequestPacket(ByteBuf buf) {
        super(buf);
    }

    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public int getPublicKeyLength() {
        return publicKeyLength;
    }

    public void setPublicKeyLength(int publicKeyLength) {
        this.publicKeyLength = publicKeyLength;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public int getVerifyTokenLength() {
        return verifyTokenLength;
    }

    public void setVerifyTokenLength(int verifyTokenLength) {
        this.verifyTokenLength = verifyTokenLength;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(byte[] verifyToken) {
        this.verifyToken = verifyToken;
    }

    @Override
    public void decode(ByteBuf buf) {
        this.serverID = ByteBufUtils.readString(buf);
        this.publicKeyLength = ByteBufUtils.readVarInt(buf);

        publicKey = new byte[publicKeyLength];
        buf.readBytes(publicKey);

        this.verifyTokenLength = ByteBufUtils.readVarInt(buf);

        verifyToken = new byte[verifyTokenLength];
        buf.readBytes(verifyToken);

        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        ByteBufUtils.writeString(buf, serverID);
        ByteBufUtils.writeVarInt(buf, publicKeyLength);
        buf.writeBytes(publicKey);
        ByteBufUtils.writeVarInt(buf, verifyTokenLength);
        buf.writeBytes(verifyToken);
        return buf;
    }

    @Override
    public String toString() {
        return "HandshakePacket[" + publicKeyLength + "," + new String(publicKey) + "," + verifyTokenLength + "," + new String(verifyToken) + "]";
    }
}

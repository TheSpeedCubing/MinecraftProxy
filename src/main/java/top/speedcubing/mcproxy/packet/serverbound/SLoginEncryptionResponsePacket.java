package top.speedcubing.mcproxy.packet.serverbound;

import io.netty.buffer.ByteBuf;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.mcproxy.packet.PacketUtils;
import top.speedcubing.mcproxy.packet.ServerboundPacket;

public class SLoginEncryptionResponsePacket extends ServerboundPacket {
    private int shardSecretLength;
    private byte[] shardSecret;
    private int verifyTokenLength;
    private byte[] verifyToken;


    public SLoginEncryptionResponsePacket(ByteBuf buf) {
        super(buf);
    }

    public int getShardSecretLength() {
        return shardSecretLength;
    }

    public void setShardSecretLength(int shardSecretLength) {
        this.shardSecretLength = shardSecretLength;
    }

    public byte[] getShardSecret() {
        return shardSecret;
    }

    public void setShardSecret(byte[] shardSecret) {
        this.shardSecret = shardSecret;
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

        this.shardSecretLength = ByteBufUtils.readVarInt(buf);

        shardSecret = new byte[shardSecretLength];
        buf.readBytes(shardSecret);

        this.verifyTokenLength = ByteBufUtils.readVarInt(buf);

        verifyToken = new byte[verifyTokenLength];
        buf.readBytes(verifyToken);

        PacketUtils.checkReadable(buf);
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = createBuf();
        ByteBufUtils.writeVarInt(buf, shardSecretLength);
        buf.writeBytes(shardSecret);
        ByteBufUtils.writeVarInt(buf, verifyTokenLength);
        buf.writeBytes(verifyToken);
        return buf;
    }

    @Override
    public String toString() {
        return "HandshakePacket[" + shardSecretLength + "," + new String(shardSecret) + "," + verifyTokenLength + "," + new String(verifyToken) + "]";
    }
}

package top.speedcubing.mcproxy.event;

import io.netty.channel.ChannelHandlerContext;
import top.speedcubing.mcproxy.config;
import top.speedcubing.mcproxy.packet.serverbound.SLoginEncryptionResponsePacket;
import top.speedcubing.mcproxy.packet.serverbound.ServerboundPacket;
import top.speedcubing.mcproxy.session.Session;

public class ServerBoundEvent {
    public static void handleServerBoundPacket(Session session, ChannelHandlerContext ctx, ServerboundPacket packet) throws Exception {
        if (packet instanceof SLoginEncryptionResponsePacket) {

            /*
            SLoginEncryptionResponsePacket p = (SLoginEncryptionResponsePacket) packet;
            byte[] s = EncryptionUtils.decryptRsa(session.keyPair, p.getShardSecret());
            session.secretKey = new SecretKeySpec(s, "AES/CFB8");

            byte[] verifyToken = EncryptionUtils.decryptRsa(session.keyPair, p.getVerifyToken());

            p.setShardSecret(EncryptionUtils.encryptRSA(session.serverPublicKey, session.secretKey.getEncoded()));
            p.setVerifyToken(EncryptionUtils.encryptRSA(session.serverPublicKey, verifyToken));
            */

            //temp solution
            if (config.readDetail) {
                session.clientChannel.pipeline().remove("frame-decoder");
                session.serverChannel.pipeline().remove("frame-decoder");
                session.encryptMode = true;
            }
        }
    }
}

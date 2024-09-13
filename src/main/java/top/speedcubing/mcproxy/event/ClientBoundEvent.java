package top.speedcubing.mcproxy.event;

import io.netty.channel.ChannelHandlerContext;
import top.speedcubing.mcproxy.packet.clientbound.CLoginEncryptionRequestPacket;
import top.speedcubing.mcproxy.packet.ClientboundPacket;
import top.speedcubing.mcproxy.session.Session;

public class ClientBoundEvent {

    public static void handleClientBoundPacket(Session session, ChannelHandlerContext ctx, ClientboundPacket packet) throws Exception {
        if (packet instanceof CLoginEncryptionRequestPacket) {

            /*
            CLoginEncryptionRequestPacket p = (CLoginEncryptionRequestPacket) packet;

//            //save public key from server
            session.serverPublicKey = p.getPublicKey();
//
//            //modify public key of proxy->client
            p.setPublicKey(session.keyPair.getPublic().getEncoded());
            */
        }
    }
}

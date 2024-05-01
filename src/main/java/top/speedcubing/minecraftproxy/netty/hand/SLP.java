package top.speedcubing.minecraftproxy.netty.hand;

public class SLP {
    private int packetLength;
    private int packetID;
    private Handshake handshake;

    public SLP(int packetLength, int packetID, Handshake handshake) {
        this.packetLength = packetLength;
        this.packetID = packetID;
        this.handshake = handshake;
    }

    public int getPacketLength() {
        return packetLength;
    }

    public int getPacketID() {
        return packetID;
    }

    public Handshake getHandshake() {
        return handshake;
    }
}

package top.speedcubing.minecraftproxy.netty.hand;

public class Handshake {

    private final int protocolVersion;
    private final String serverAddress;
    private final int serverPort;
    private final int nextState;

    public Handshake(int protocolVersion, String serverAddress, int serverPort, int nextState) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nextState = nextState;
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
}

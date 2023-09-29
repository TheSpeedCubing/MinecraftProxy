package top.speedcubing.minecraftproxy.events;

import top.speedcubing.lib.eventbus.CubingEvent;
import top.speedcubing.minecraftproxy.obj.Node;
import top.speedcubing.minecraftproxy.obj.ServerPing;

import java.net.InetSocketAddress;

public class ServerListPingEvent extends CubingEvent {
    private final Node node;
    private final InetSocketAddress address;
    private ServerPing serverPing;

    public ServerListPingEvent(Node node, InetSocketAddress address) {
        this.node = node;
        this.address = address;
    }

    public Node getNode() {
        return node;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public ServerPing getServerPing() {
        return serverPing;
    }

    public void setServerPing(ServerPing serverPing) {
        this.serverPing = serverPing;
    }
}

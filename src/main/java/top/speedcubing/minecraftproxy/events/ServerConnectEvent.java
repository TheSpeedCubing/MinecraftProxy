package top.speedcubing.minecraftproxy.events;

import top.speedcubing.lib.eventbus.CubingEvent;
import top.speedcubing.minecraftproxy.obj.Node;

import java.net.InetSocketAddress;

public class ServerConnectEvent extends CubingEvent {
    private final Node node;
    private final InetSocketAddress address;
    private String kickMessage = null;

    public ServerConnectEvent(Node node, InetSocketAddress address) {
        this.node = node;
        this.address = address;
    }

    public Node getNode() {
        return node;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void kick(String kickMessage) {
        this.kickMessage = kickMessage;
    }
}

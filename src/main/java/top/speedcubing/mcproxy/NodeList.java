package top.speedcubing.mcproxy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import top.speedcubing.mcproxy.server.Node;

public class NodeList {
    public static List<Node> nodes = new ArrayList<>();

    public static Node getNodeByAddress(InetSocketAddress address) {
        for (Node n : nodes) {
            if (n.getAddress().equals(address)) {
                return n;
            }
        }
        return null;
    }

    public static void removeNodeByAddress(InetSocketAddress address) {
        Iterator<Node> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            Node n = iterator.next();
            if (n.getAddress().equals(address)) {
                iterator.remove();
            }
        }
    }

    public static void add(Node n) {
        nodes.add(n);
    }
}

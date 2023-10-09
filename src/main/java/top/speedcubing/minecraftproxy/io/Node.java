package top.speedcubing.minecraftproxy.io;

import java.net.ServerSocket;

public class Node {
    public String name;
    public int localPort;
    public String remoteHost;
    public int remotePort;
    public String HAProxy = null;
    public Thread thread;
    public ServerSocket socket;

    public Node(String name, int localPort, String remoteAddress, String HAProxy) {
        this.name = name;
        this.localPort = localPort;
        String[] s = remoteAddress.split(":");
        this.remoteHost = s[0];
        this.remotePort = s.length == 1 ? 25565 : Integer.parseInt(s[1]);
        switch (HAProxy) {
            case "v1":
                this.HAProxy = "v1";
                break;
            case "v2":
                this.HAProxy = "v2";
        }
    }

    public String toString() {
        return name;
    }
}

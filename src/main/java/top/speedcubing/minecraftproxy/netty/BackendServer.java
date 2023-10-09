package top.speedcubing.minecraftproxy.netty;

import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;

public class BackendServer {
    public String ip;
    public int port;
    public HAProxyProtocolVersion HAProxy;

    public BackendServer(String address,String HAProxy){
        String[] s = address.split(":");
        this.ip = s[0];
        this.port = s.length == 1 ? 25565 : Integer.parseInt(s[1]);
        switch (HAProxy) {
            case "v1":
                this.HAProxy = HAProxyProtocolVersion.V1;
                break;
            case "v2":
                this.HAProxy = HAProxyProtocolVersion.V2;
                break;
        }
    }
}

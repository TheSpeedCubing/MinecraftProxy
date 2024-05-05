package top.speedcubing.mcproxy.server;

import com.google.gson.JsonObject;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;

public class BackendServer {
    public String ip;
    public int port;
    public final boolean tcpFastOpen;
    public final int readTimeout;
    public HAProxyProtocolVersion HAProxy;

    public BackendServer(JsonObject o) {
        String address = o.get("address").getAsString();
        String[] s = address.split(":");
        this.ip = s[0];
        this.port = s.length == 1 ? 25565 : Integer.parseInt(s[1]);

        this.tcpFastOpen = o.get("tcpFastOpen").getAsBoolean();
        this.readTimeout = o.get("readTimeout").getAsInt();

        switch (o.getAsJsonObject().get("proxy-protocol").getAsString()) {
            case "v1":
                this.HAProxy = HAProxyProtocolVersion.V1;
                break;
            case "v2":
                this.HAProxy = HAProxyProtocolVersion.V2;
                break;
        }
    }
}

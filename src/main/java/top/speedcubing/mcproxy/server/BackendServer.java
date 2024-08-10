package top.speedcubing.mcproxy.server;

import com.google.gson.JsonObject;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import java.net.InetSocketAddress;
import top.speedcubing.mcproxy.Main;

public class BackendServer {
    public InetSocketAddress address;
    public final int readTimeout;
    public HAProxyProtocolVersion HAProxy;

    public BackendServer(JsonObject o) {
        this.address = Main.parseAddress(o.get("address").getAsString());

        this.readTimeout = o.get("readTimeout").getAsInt();

        switch (o.get("proxy-protocol").getAsString()) {
            case "v1":
                this.HAProxy = HAProxyProtocolVersion.V1;
                break;
            case "v2":
                this.HAProxy = HAProxyProtocolVersion.V2;
                break;
        }
    }
}

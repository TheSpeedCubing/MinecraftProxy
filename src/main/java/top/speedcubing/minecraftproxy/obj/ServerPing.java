package top.speedcubing.minecraftproxy.obj;

import java.util.UUID;

public class ServerPing {
    public String versionName;
    public int protocol;
    public int playerMax;
    public int playerOnline;
    public String[] players;
    public UUID[] uuids;
    public String favicon;
    public String text;

    public ServerPing(String versionName, int protocol, int playerMax, int playerOnline, String[] players, UUID[] uuids, String favicon, String text) {
        this.versionName = versionName;
        this.protocol = protocol;
        this.playerMax = playerMax;
        this.playerOnline = playerOnline;
        this.players = players;
        this.uuids = uuids;
        this.favicon = favicon;
        this.text = text;
    }
}

package top.speedcubing.mcproxy.server;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class NodeSetting {
    private final Map<String, Setting> settings = new HashMap<>();

    public NodeSetting(JsonObject o) {
        settings.put("name", new Setting(o.get("name").getAsString(), false));
        settings.put("log", new Setting(o.get("log").getAsBoolean(), false));
        settings.put("blockedCIDR", new Setting(o.get("blockedCIDR").getAsJsonArray(), false));
        settings.put("tcpFastOpen", new Setting(o.get("tcpFastOpen").getAsBoolean(), true));
        settings.put("readTimeout", new Setting(o.get("readTimeout").getAsInt(), true));
        settings.put("servers", new Setting(o.get("servers").getAsJsonArray(), false));
        settings.put("disableNativeTransport", new Setting(o.get("disableNativeTransport").getAsBoolean(), true));
    }

    public Map<String, Setting> getSettings() {
        return settings;
    }

    public boolean requireRestart(NodeSetting other) {
        boolean restart = false;
        for (String key : getSettings().keySet()) {
            if (getSettings().get(key).requireStart()) {
                if (!other.getSettings().get(key).equals(getSettings().get(key))) {
                    restart = true;
                    break;
                }
            }
        }
        return restart;
    }
}

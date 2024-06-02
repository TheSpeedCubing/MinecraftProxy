package top.speedcubing.mcproxy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import top.speedcubing.mcproxy.server.Node;
import top.speedcubing.mcproxy.server.NodeSetting;

public class config {

    public static void move() {
        try {
            if (!new File("config.json").exists()) {
                InputStream file = Main.class.getClassLoader().getResourceAsStream("config.json");
                Files.copy(file, Paths.get("config.json"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void reload() {
        try {
            JsonObject object = JsonParser.parseReader(new FileReader("config.json")).getAsJsonObject();

            //replace it as new nodes
            for (JsonElement j : object.get("nodes").getAsJsonArray()) {
                JsonObject o = j.getAsJsonObject();
                if (o.get("state").getAsBoolean()) {
                    InetSocketAddress address = new InetSocketAddress(o.get("address").getAsString(), o.get("port").getAsInt());

                    NodeSetting nodeSetting = new NodeSetting(o);

                    Node foundNode = NodeList.getNodeByAddress(address);

                    boolean createNew = true;

                    if (foundNode != null) {
                        createNew = foundNode.loadSettings(nodeSetting);
                    }

                    if (createNew) {
                        if(foundNode != null){
                            foundNode.shutdown();
                        }
                        NodeList.removeNodeByAddress(address);
                        Node n = new Node(address, nodeSetting);
                        NodeList.add(n);
                        n.startup();
                    } else {
                        Main.print("Loaded Settings for " + foundNode);
                    }
                }
            }

        } catch (Exception e) {
            Main.print("config.json error");
            e.printStackTrace();
        }
    }
}

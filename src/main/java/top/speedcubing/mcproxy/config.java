package top.speedcubing.mcproxy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import top.speedcubing.mcproxy.server.Node;

public class config {
    public static List<Node> nodeSet = new ArrayList<>();

    public static void reload() {
        List<Node> copiedNodes = new ArrayList<>(nodeSet);
        try {
            if (!new File("config.json").exists()) {
                InputStream file = Main.class.getClassLoader().getResourceAsStream("config.json");
                Files.copy(file, Paths.get("config.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            JsonObject object = JsonParser.parseReader(new FileReader("config.json")).getAsJsonObject();

            nodeSet.clear();
            for (JsonElement j : object.get("nodes").getAsJsonArray()) {
                JsonObject o = j.getAsJsonObject();
                if (o.get("state").getAsBoolean()) {
                    Node n = new Node(o);
                    nodeSet.add(n);
                }
            }
            for (Node n : copiedNodes) {
                n.bossGroup.shutdownGracefully();
                n.workerGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            Main.print("config.json error");
            nodeSet.clear();
            nodeSet = copiedNodes;
            e.printStackTrace();
        }
        for (final Node n : nodeSet) {
            try {
                n.createBootstrap();
                Main.print("Created ServerBootstrap for " + n);
            } catch (Exception e) {
                n.bossGroup.shutdownGracefully();
                n.workerGroup.shutdownGracefully();
                e.printStackTrace();
            }
        }
    }
}
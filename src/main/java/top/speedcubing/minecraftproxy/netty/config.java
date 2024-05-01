package top.speedcubing.minecraftproxy.netty;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.minecraftproxy.netty.handler.CIDRBlockingHandler;
import top.speedcubing.minecraftproxy.netty.handler.ConnectionHandler;

public class config {

    public static boolean serverpingLog = true;
    public static boolean connectingLog = true;
    public static List<Node> nodeSet = new ArrayList<>();

    public static void reload() {
        List<Node> copiedNodes = new ArrayList<>(nodeSet);
        try {
            if (!new File("config.json").exists()) {
                InputStream file = Main.class.getClassLoader().getResourceAsStream("config.json");
                Files.copy(file, Paths.get("config.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            JsonObject object = JsonParser.parseReader(new FileReader("config.json")).getAsJsonObject();
            serverpingLog = object.getAsJsonObject("log").get("serverping").getAsBoolean();
            connectingLog = object.getAsJsonObject("log").get("connection").getAsBoolean();

            nodeSet.clear();
            for (JsonElement j : object.get("nodes").getAsJsonArray()) {
                JsonObject o = j.getAsJsonObject();
                if (o.get("state").getAsBoolean()) {
                    List<BackendServer> backends = new ArrayList<>();
                    o.get("servers").getAsJsonArray().forEach(a -> backends.add(new BackendServer(a.getAsJsonObject().get("address").getAsString(), a.getAsJsonObject().get("proxy-protocol").getAsString())));
                    Set<CIDR> blockedCIDR = new HashSet<>();
                    o.get("blockedCIDR").getAsJsonArray().forEach(a -> blockedCIDR.add(new CIDR(a.getAsString())));
                    Node n = new Node(
                            o.get("name").getAsString(),
                            o.get("localport").getAsInt(),
                            backends,
                            blockedCIDR,
                            o.get("noConnection").getAsBoolean(),
                            o.get("kick").getAsBoolean(),
                            o.get("kickMessage").getAsString()
                    );
                    nodeSet.add(n);
                }
            }
            for (Node n : copiedNodes) {
                n.bossGroup.shutdownGracefully();
                n.workerGroup.shutdownGracefully();
                n.future.channel().close();
            }
        } catch (Exception e) {
            Main.print("config.json error");
            nodeSet.clear();
            nodeSet = copiedNodes;
            e.printStackTrace();
        }
        for (final Node n : nodeSet) {
            try {
                n.future = new ServerBootstrap()
                        .group(n.bossGroup, n.workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1 << 20, 1 << 21))
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.IP_TOS, 0x18)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new CIDRBlockingHandler(n));
                                ch.pipeline().addLast(new ConnectionHandler(n));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true).bind(n.localport).sync();
                Main.print("Created ServerBootstrap for " + n);
            } catch (Exception e) {
                n.bossGroup.shutdownGracefully();
                n.workerGroup.shutdownGracefully();
                n.future.channel().close();
                e.printStackTrace();
            }
        }
    }
}

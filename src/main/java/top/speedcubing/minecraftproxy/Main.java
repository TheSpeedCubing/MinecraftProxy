package top.speedcubing.minecraftproxy;

import com.google.gson.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.speedcubing.minecraftproxy.obj.Node;

import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static boolean serverpingLog = true;
    public static boolean connectingLog = true;
    public static String offlinePing;
    public static String offlineKick;
    public static List<Node> nodeSet = new ArrayList<>();

    public static void print(String s) {
        System.out.println("[" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + "] [MinecraftProxy] " + s);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while (scanner.hasNext()) {
                switch (scanner.nextLine()) {
                    case "reload":
                        reload();
                        break;
                }
            }
        }).start();
        print("loading...");
        reload();
    }

    public static void reload() {
        List<Node> copiedNodes = new ArrayList<>(nodeSet);
        try {
            nodeSet.clear();
            JsonObject object = JsonParser.parseReader(new FileReader("config.json")).getAsJsonObject();
            serverpingLog = object.getAsJsonObject("log").get("serverping").getAsBoolean();
            connectingLog = object.getAsJsonObject("log").get("connection").getAsBoolean();
            offlinePing = object.get("offline-ping").getAsJsonObject().toString();
            offlineKick = object.get("offline-kick").getAsString();
            JsonArray nodes = object.get("nodes").getAsJsonArray();
            for (JsonElement j : nodes) {
                JsonObject o = j.getAsJsonObject();
                if (o.get("state").getAsBoolean()) {
                    Node n = new Node(
                            o.get("name").getAsString(),
                            o.get("localPort").getAsInt(),
                            o.get("remoteAddress").getAsString(),
                            o.get("proxy-protocol").getAsString()
                    );
                    nodeSet.add(n);
                }
            }
            for (Node n : copiedNodes)
                n.future.channel().close();
        } catch (Exception e) {
            print("config.json error");
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
                                ch.pipeline().addLast(new ConnectionHandler(n));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true).bind(n.localPort).sync();
                print("Created ServerBootstrap for " + n);
            } catch (Exception e) {
                n.bossGroup.shutdownGracefully();
                n.workerGroup.shutdownGracefully();
                e.printStackTrace();
            }
        }
    }
}
package top.speedcubing.minecraftproxy.netty;

import com.google.gson.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.speedcubing.lib.utils.SystemUtils;
import top.speedcubing.lib.utils.internet.ip.CIDR;

import java.io.FileReader;
import java.lang.management.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static Random random = new Random();
    public static boolean serverpingLog = true;
    public static boolean connectingLog = true;
    public static List<Node> nodeSet = new ArrayList<>();

    public static void print(String s) {
        System.out.println("[" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + "] [MinecraftProxy] " + s);
    }

    public static void main(String[] args) {
        print("loading netty proxy...");
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while (scanner.hasNext()) {
                switch (scanner.nextLine()) {
                    case "reload":
                        reload();
                        break;
                    case "heap":
                        MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                        System.out.println("Used: " + usage.getUsed() / 1048576 + " (" + usage.getUsed() + "), Heap: " + usage.getCommitted() / 1048576 + ", Max: " + SystemUtils.getXmx() / 1048576 + " (" + (double) usage.getUsed() * 100 / SystemUtils.getXmx() + ")");
                        break;
                    case "gc":
                        System.gc();
                        break;
                    case "print":
                        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                        System.out.println(threadSet);
                        break;
                }
            }
        }).start();
        reload();
    }

    public static void reload() {
        List<Node> copiedNodes = new ArrayList<>(nodeSet);
        try {
            nodeSet.clear();
            JsonObject object = JsonParser.parseReader(new FileReader("config.json")).getAsJsonObject();
            serverpingLog = object.getAsJsonObject("log").get("serverping").getAsBoolean();
            connectingLog = object.getAsJsonObject("log").get("connection").getAsBoolean();
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
                            blockedCIDR
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
                                ch.pipeline().addLast(new ConnectionHandler(n, System.currentTimeMillis()));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true).bind(n.localport).sync();
                print("Created ServerBootstrap for " + n);
            } catch (Exception e) {
                n.bossGroup.shutdownGracefully();
                n.workerGroup.shutdownGracefully();
                e.printStackTrace();
            }
        }
    }
}
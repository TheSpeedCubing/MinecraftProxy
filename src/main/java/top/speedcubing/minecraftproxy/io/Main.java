package top.speedcubing.minecraftproxy.io;

import com.google.gson.*;
import top.speedcubing.lib.utils.SystemUtils;

import java.io.*;
import java.lang.management.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static boolean serverpingLog = true;
    public static boolean connectingLog = true;
    public static String offlinePing;
    public static String offlineKick;
    public static List<Node> nodeSet = new ArrayList<>();
    public static ScheduledThreadPoolExecutor ex;

     public static void print(String s) {
        System.out.println("[" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + "] [MinecraftProxy] " + s);
    }

    public static void main(String[] args) {
        try {
            ex = new ScheduledThreadPoolExecutor(32);
        } catch (Exception e) {
            e.printStackTrace();
        }
        print("loading socket proxy...");
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while (scanner.hasNext()) {
                switch (scanner.nextLine()) {
                    case "reload":
                        reload();
                        break;
                    case "heap":
                        MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                        System.out.println("Used: " + usage.getUsed() + ", Heap: " + usage.getCommitted() / 1048576 + ", Max: " + SystemUtils.getXmx() / 1048576 + " (" + (double) usage.getUsed() / SystemUtils.getXmx() + ")");
                        break;
                    case "gc":
                        System.gc();
                        break;
                }
            }
        }).start();
        reload();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
//                try {
//                    Set<ClientToProxyConnection> toR = new HashSet<>();
//                    for (Map.Entry<ClientToProxyConnection, ProxyToServerConnection> e : connectionMap.entrySet()) {
//                        if (!e.getKey().active) {
//                            if (e.getValue() == null || !e.getValue().active)
//                                toR.add(e.getKey());
//                        }
//                    }
//                    for (ClientToProxyConnection c : toR) {
//                        connectionMap.remove(c);
//                    }
//                    System.out.println("-------------------------");
//                    for (Map.Entry<ClientToProxyConnection, ProxyToServerConnection> e : connectionMap.entrySet()) {
//                        System.out.println(e.getKey().socket.getInetAddress().getHostAddress() + " " + e.getKey().active + " " + (e.getValue() == null ? "-1" : e.getValue().n.name) + " " + (e.getValue() == null ? "-1" : e.getValue().active));
//                    }
//                } catch (Exception ignored) {
//                    ignored.printStackTrace();
//                }
//                Set<Thread> toR = new HashSet<>();
//                for (Thread t : threads) {
//                    if (!t.isAlive()) {
//                        toR.add(t);
//                    }
//                }
//                threads.removeAll(toR);
//                System.out.println(threads.size()+" "+threads);
            }
        }, 0, 1000);
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
            for (Node n : copiedNodes) {
                n.thread.interrupt();
                n.socket.close();
            }
        } catch (Exception e) {
            print("config.json error");
            nodeSet.clear();
            nodeSet = copiedNodes;
            e.printStackTrace();
        }
        for (final Node n : nodeSet) {
            try {
                n.socket = new ServerSocket(n.localPort);
                n.thread = new Thread(() -> {
                    while (true) {
                        try {
                            Socket client = n.socket.accept();
                            ClientToProxyConnection connection = new ClientToProxyConnection(client, n);
                            connection.future = ex.schedule(connection, 0, TimeUnit.MILLISECONDS);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                n.thread.start();
                print("Created Server for " + n);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

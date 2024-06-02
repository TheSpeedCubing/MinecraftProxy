package top.speedcubing.mcproxy;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.Set;
import top.speedcubing.lib.utils.SystemUtils;

public class Main {

    public static void print(Object o) {
        System.out.println("[" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + "] [MinecraftProxy] " + o);
    }

    public static void main(String[] args) {
        print("loading netty proxy...");
        Scanner scanner = new Scanner(System.in);
        config.move();
        config.reload();
        new Thread(() -> {
            while (scanner.hasNext()) {
                switch (scanner.nextLine()) {
                    case "reload":
                        config.reload();
                        break;
                    case "heap":
                        MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                        Main.print("Used: " + usage.getUsed() / 1048576 + " (" + usage.getUsed() + "), Heap: " + usage.getCommitted() / 1048576 + ", Max: " + SystemUtils.getXmx() / 1048576 + " (" + (double) usage.getUsed() * 100 / SystemUtils.getXmx() + ")");
                        break;
                    case "gc":
                        System.gc();
                        break;
                    case "print":
                        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                        Main.print(threadSet);
                        break;
                    case "end":
                        Main.print("shutting down");
                        System.exit(0);
                        break;
                }
            }
        }).start();
    }
}
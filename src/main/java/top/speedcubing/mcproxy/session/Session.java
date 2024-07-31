package top.speedcubing.mcproxy.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import java.net.SocketException;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.server.Node;

public class Session {
    public final Node node;
    public volatile Channel serverChannel;
    public volatile Channel clientChannel;
    public volatile ChannelHandlerContext serverHandler;
    public volatile ChannelHandlerContext clientHandler;
    private volatile boolean closed = false;

    public Session(Node node) {
        //System.out.println("Session " + this.hashCode() + " create");
        node.sessionCount++;
        this.node = node;
    }

    public void handleException(Throwable cause, String error) {
        close();
        if (cause instanceof ReadTimeoutException)
            return;
        if (cause instanceof SocketException && cause.getMessage().equals("Connection reset"))
            return;
        Main.print("(" + error + ") ");
        cause.printStackTrace();
    }

    public synchronized void close() {;
        if (closed) {
            return;
        }

        //System.out.println("Session " + this.hashCode() + " close");

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (clientChannel != null) {
            clientChannel.close();
        }

        if (serverHandler != null) {
            serverHandler.close();
        }

        if (clientHandler != null) {
            clientHandler.close();
        }

        this.closed = true;
        node.sessionCount--;
    }
}

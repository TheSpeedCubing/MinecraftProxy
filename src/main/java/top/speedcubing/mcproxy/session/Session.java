package top.speedcubing.mcproxy.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.server.Node;

public class Session {
    public final Node node;
    public volatile Channel serverChannel;
    public volatile Channel clientChannel;
    public volatile boolean closed = false;

    public Session(Node node) {
        node.sessionCount++;
        this.node = node;
    }

    public void handleException(ChannelHandlerContext ctx, Throwable cause, String error) {
        close(ctx);
        if (cause instanceof ReadTimeoutException)
            return;
        Main.print("(" + error + ") ");
        cause.printStackTrace();
    }

    public void close(ChannelHandlerContext... contexts) {
        for (ChannelHandlerContext c : contexts) {
            c.close();
            c.channel().close();
        }

        if (closed)
            return;

        if (serverChannel != null && serverChannel.isOpen())
            serverChannel.close();

        if (clientChannel != null && clientChannel.isOpen())
            clientChannel.close();

        this.closed = true;
        node.sessionCount--;
    }
}

package top.speedcubing.minecraftproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import top.speedcubing.minecraftproxy.server.Node;

public class ServerChannelInitializer extends ChannelInitializer<Channel> {

    private final Node node;

    public ServerChannelInitializer(Node node) {
        this.node = node;
    }

    @Override
    public void initChannel(Channel ch) {
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(node.readTimeout, TimeUnit.MILLISECONDS));
        ch.pipeline().addLast("minecraft-decoder", new ConnectionHandler(node));
    }
}

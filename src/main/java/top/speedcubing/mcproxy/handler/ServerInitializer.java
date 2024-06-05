package top.speedcubing.mcproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import top.speedcubing.mcproxy.Main;

public class ServerInitializer extends ChannelInitializer<Channel> {
    private final ClientHandler clientHandler;
    private final Channel clientChannel;

    public ServerInitializer(ClientHandler clientHandler, Channel clientChannel) {
        this.clientHandler = clientHandler;
        this.clientChannel = clientChannel;
    }

    @Override
    public void initChannel(Channel ch) {
        ch.pipeline().addLast("server-handler", new ServerHandler(clientHandler, clientChannel));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clientHandler.closeEverything(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Main.print("(ServerInitializer) ");
        cause.printStackTrace();
        clientHandler.closeEverything(ctx);
    }
}

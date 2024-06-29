package top.speedcubing.mcproxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import top.speedcubing.mcproxy.Main;

public class ServerHandler extends ChannelInboundHandlerAdapter {


    private final ClientHandler clientHandler;
    private final Channel clientChannel;

    ServerHandler(ClientHandler clientHandler, Channel clientChannel) {
        this.clientHandler = clientHandler;
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        clientChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clientHandler.closeEverything(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Main.print("(ServerHandler) ");
        cause.printStackTrace();
        clientHandler.closeEverything(ctx);
    }
}

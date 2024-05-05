package top.speedcubing.mcproxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import java.net.InetSocketAddress;
import java.util.Random;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.server.BackendServer;
import top.speedcubing.mcproxy.server.Node;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final BackendServer server;
    private final Node node;
    private boolean isConnectedToServer = false;
    private EventLoopGroup eventLoop = null;
    private Channel serverChannel;


    public ClientHandler(Node node) {
        Random r = new Random();
        this.node = node;
        this.server = node.servers.get(r.nextInt(node.servers.size()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //CIDR blocking
        if (!isConnectedToServer) {
            InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            for (CIDR cidr : node.blockedCIDR) {
                if (cidr.contains(playerAddress.getAddress().getHostAddress())) {
                    closeEverything(ctx);
                    return;
                }
            }
        }

        ByteBuf buf = (ByteBuf) msg;

        if (buf.readableBytes() == 0)
            return;

        if (!isConnectedToServer) {
            try {
                connectToServer(ctx);
                isConnectedToServer = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        serverChannel.writeAndFlush(buf);

    }

    void connectToServer(ChannelHandlerContext clientHandler) throws InterruptedException {
        eventLoop = new NioEventLoopGroup();
        long start = System.currentTimeMillis();
        Bootstrap bootstrap = new Bootstrap()
                .channelFactory(node.transportType.socketChannelFactory)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, server.readTimeout)
                .group(eventLoop)
                .handler(new ServerInitializer(this, clientHandler.channel()));

//        if (server.tcpFastOpen)
//                    bootstrap.option(ChannelOption.TCP_FASTOPEN, 3);
        ChannelFuture future = bootstrap.connect(server.ip, server.port).sync();
        this.serverChannel = future.channel();

        if (!future.isSuccess()) {
            closeEverything(clientHandler);
        } else {
            long ping = System.currentTimeMillis() - start;

            InetSocketAddress playerAddress = (InetSocketAddress) clientHandler.channel().remoteAddress();

            if (server.HAProxy != null) {
                this.serverChannel.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                InetSocketAddress serverAddress = (InetSocketAddress) serverChannel.remoteAddress();
                HAProxyMessage haProxyMessage = new HAProxyMessage(server.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, playerAddress.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), playerAddress.getPort(), serverAddress.getPort());
                serverChannel.writeAndFlush(haProxyMessage);
                serverChannel.pipeline().remove(HAProxyMessageEncoder.INSTANCE);
            }

            if (node.log)
                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " connected (" + ping + "ms)");
        }
    }

    public void closeEverything(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.close();
        channelHandlerContext.channel().close();

        if (eventLoop != null)
            eventLoop.shutdownGracefully();
        if (serverChannel != null)
            serverChannel.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeEverything(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Main.print("(1) ");
        cause.printStackTrace();
        closeEverything(ctx);
    }
}

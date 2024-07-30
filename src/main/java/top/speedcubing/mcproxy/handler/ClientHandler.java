package top.speedcubing.mcproxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import java.net.InetSocketAddress;
import java.util.Random;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.mcproxy.Main;
import top.speedcubing.mcproxy.server.BackendServer;
import top.speedcubing.mcproxy.session.Session;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final BackendServer server;
    private boolean isConnectedToServer = false;
    public final Session session;

    ClientHandler(Session session) {
        Random r = new Random();
        this.session = session;
        this.server = session.node.servers.get(r.nextInt(session.node.servers.size()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        //CIDR blocking
        if (!isConnectedToServer) {
            InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            for (CIDR cidr : session.node.blockedCIDR) {
                if (cidr.contains(playerAddress.getAddress().getHostAddress())) {
                    session.close(ctx);
                    ReferenceCountUtil.release(msg);
                    return;
                }
            }
        }

        if (!isConnectedToServer) {
            try {
                connectToServer(ctx);
                isConnectedToServer = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        session.serverChannel.writeAndFlush(msg);
    }

    void connectToServer(ChannelHandlerContext clientHandler) throws InterruptedException {
        session.clientChannel = clientHandler.channel();

        long start = System.currentTimeMillis();

        Bootstrap bootstrap = new Bootstrap()
                .channelFactory(session.node.transportType.socketChannelFactory)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, server.readTimeout)
                .group(session.node.clientWorkerGroup)
                .handler(new ServerInitializer(session));

        ChannelFuture future = bootstrap.connect(server.ip, server.port).sync();
        session.serverChannel = future.channel();

        if (!future.isSuccess()) {
            session.close(clientHandler);
            return;
        }

        long ping = System.currentTimeMillis() - start;

        InetSocketAddress playerAddress = (InetSocketAddress) clientHandler.channel().remoteAddress();

        if (server.HAProxy != null) {
            session.serverChannel.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
            InetSocketAddress serverAddress = (InetSocketAddress) session.serverChannel.remoteAddress();
            HAProxyMessage haProxyMessage = new HAProxyMessage(server.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, playerAddress.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), playerAddress.getPort(), serverAddress.getPort());
            session.serverChannel.writeAndFlush(haProxyMessage);
            session.serverChannel.pipeline().remove(HAProxyMessageEncoder.INSTANCE);
        }

        if (session.node.getSetting("log").getAsBoolean())
            Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + session.node + " connected (" + ping + "ms)");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        session.close(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        session.handleException(ctx, cause, "ClientHandler");
    }
}

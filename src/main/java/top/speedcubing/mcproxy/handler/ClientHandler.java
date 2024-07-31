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
    public void handlerAdded(ChannelHandlerContext ctx) {
        session.clientHandler = ctx;
        session.clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //CIDR blocking
        if (!isConnectedToServer) {
            InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            for (CIDR cidr : session.node.blockedCIDR) {
                if (cidr.contains(playerAddress.getAddress().getHostAddress())) {
                    session.close();
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

                session.close();
                ReferenceCountUtil.release(msg);
                return;
            }
        }

        session.serverChannel.writeAndFlush(msg);
    }

    void connectToServer(ChannelHandlerContext clientHandler) throws InterruptedException {

        long start = System.currentTimeMillis();

        Bootstrap bootstrap = new Bootstrap()
                .channelFactory(session.node.transportType.socketChannelFactory)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, server.readTimeout)
                .group(session.node.clientWorkerGroup)
                .handler(new ServerInitializer(session));

        ChannelFuture future = bootstrap.connect(server.ip, server.port).sync();

        if (!future.isSuccess()) {
            session.close();
            return;
        }

        long ping = System.currentTimeMillis() - start;

        InetSocketAddress clientAddr = (InetSocketAddress) clientHandler.channel().remoteAddress();

        //HAProxy Protocol
        if (server.HAProxy != null) {
            session.serverChannel.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
            InetSocketAddress serverAddress = (InetSocketAddress) session.serverChannel.remoteAddress();
            HAProxyMessage haProxyMessage = new HAProxyMessage(server.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, clientAddr.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), clientAddr.getPort(), serverAddress.getPort());
            session.serverChannel.writeAndFlush(haProxyMessage);
            session.serverChannel.pipeline().remove(HAProxyMessageEncoder.INSTANCE);
        }

        if (session.node.getSetting("log").getAsBoolean())
            Main.print(clientAddr.getAddress().getHostAddress() + ":" + clientAddr.getPort() + " -> " + session.node + " connected (" + ping + "ms)");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        session.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        session.handleException(cause, "ClientHandler");
    }
}

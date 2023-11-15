package top.speedcubing.minecraftproxy.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.*;
import io.netty.util.ReferenceCountUtil;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.lib.utils.internet.ip.CIDR;

import java.net.InetSocketAddress;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private final BackendServer server;
    private final Node node;
    private EventLoopGroup eventLoop;
    private Channel serverChannel;
    public boolean handshake = true;

    public ConnectionHandler(Node node) {
        this.node = node;
        this.server = node.servers.get(Main.random.nextInt(node.servers.size()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (handshake) {
            InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            for (CIDR cidr : node.blockedCIDR) {
                if (cidr.contains(playerAddress.getAddress().getHostAddress())) {
                    close();
                    ctx.close();
                    return;
                }
            }
            ByteBuf buf = (ByteBuf) msg;
            final int packetLength = ByteBufUtils.readVarInt(buf);
            final int packetID = ByteBufUtils.readVarInt(buf);
            final int clientVersion = ByteBufUtils.readVarInt(buf);
            final String hostname = ByteBufUtils.readString(buf);
            final int port = buf.readUnsignedShort();
            final int state = ByteBufUtils.readVarInt(buf);
            forwardToServer(ctx, buf, packetLength, packetID, clientVersion, hostname, port, state, playerAddress);
        } else {
            if (serverChannel != null)
                serverChannel.writeAndFlush(msg);
            else
                ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        close();
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Main.print("(1) " + cause);
        close();
        ctx.close();
    }

    void forwardToServer(ChannelHandlerContext ctx, ByteBuf buf, int packetLength, int packetID, int clientVersion, String hostname, int port, int state, InetSocketAddress playerAddress) {
        eventLoop = new NioEventLoopGroup();
        handshake = false;
        long start = System.currentTimeMillis();
        new Bootstrap()
                .group(eventLoop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<>() {
                    @Override
                    public void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx2, Object msg) {
                                ctx.channel().writeAndFlush(msg);
                            }
                        });
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        close();
                        ctx.close();
                        buf.release();
                    }
                }).connect(server.ip, server.port).addListener((ChannelFutureListener) future -> {
                    serverChannel = future.channel();
                    InetSocketAddress serverAddress = (InetSocketAddress) serverChannel.remoteAddress();
                    if (!future.isSuccess()) {
                        close();
                        ctx.close();
                    } else {
                        long ping = System.currentTimeMillis() - start;
                        if (server.HAProxy != null) {
                            serverChannel.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                            HAProxyMessage haProxyMessage = new HAProxyMessage(server.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, playerAddress.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), playerAddress.getPort(), serverAddress.getPort());
                            serverChannel.writeAndFlush(haProxyMessage);
                            serverChannel.pipeline().remove(HAProxyMessageEncoder.INSTANCE);
                        }
                        //forward data to server
                        ByteBuf sendBuf = Unpooled.buffer();
                        ByteBufUtils.writeVarInt(sendBuf, packetLength);
                        ByteBufUtils.writeVarInt(sendBuf, packetID);
                        ByteBufUtils.writeVarInt(sendBuf, clientVersion);
                        ByteBufUtils.writeString(sendBuf, hostname);
                        ByteBufUtils.writeVarShort(sendBuf, port);
                        ByteBufUtils.writeVarInt(sendBuf, state);
                        while (buf.readableBytes() > 0)
                            sendBuf.writeByte(buf.readByte());
                        serverChannel.writeAndFlush(sendBuf);
                        if (state == 1) {
                            if (Main.serverpingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " pinged (" + ping + "ms)");
                        } else {
                            if (Main.connectingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " connected (" + ping + "ms)");
                        }
                    }
                    buf.release();
                });
    }

    private void close() {
        if (eventLoop != null)
            eventLoop.shutdownGracefully();
        if (serverChannel != null)
            serverChannel.close();
    }

//    private void send(ChannelHandlerContext ctx, String data) {
//        ByteBuf buf = Unpooled.buffer();
//        Utils.writeVarInt(buf, 0);
//        Utils.writeString(buf, data);
//        ByteBuf header = Unpooled.buffer();
//        Utils.writeVarInt(header, buf.readableBytes());
//        ctx.channel().writeAndFlush(header);
//        ctx.channel().writeAndFlush(buf);
//        ctx.close();
//    }
}

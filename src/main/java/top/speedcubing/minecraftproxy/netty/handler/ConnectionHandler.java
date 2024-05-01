package top.speedcubing.minecraftproxy.netty.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.util.Random;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.minecraftproxy.netty.BackendServer;
import top.speedcubing.minecraftproxy.netty.Main;
import top.speedcubing.minecraftproxy.netty.Node;
import top.speedcubing.minecraftproxy.netty.config;
import top.speedcubing.minecraftproxy.netty.hand.Handshake;
import top.speedcubing.minecraftproxy.netty.hand.SLP;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private final BackendServer server;
    private final Node node;
    private EventLoopGroup eventLoop = null;
    private Channel serverChannel;
    public int handshakeProgress = 1;
    private SLP slp;

    public ConnectionHandler(Node node) {
        Random r = new Random();
        this.node = node;
        this.server = node.servers.get(r.nextInt(node.servers.size()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        ByteBuf buf = (ByteBuf) msg;
        if (handshakeProgress == 1) {
            final int packetLength = ByteBufUtils.readVarInt(buf);
            final int packetID = ByteBufUtils.readVarInt(buf);

            final int protocolVersion = ByteBufUtils.readVarInt(buf);
            final String serverAddress = ByteBufUtils.readString(buf);
            final int serverPort = buf.readUnsignedShort();
            final int nextState = ByteBufUtils.readVarInt(buf);

            if (nextState != 1) {
                if (node.kick) {
                    send(ctx, "\"" + node.kickMessage + "\"");
                    return;
                }
            }
            Handshake handshake = new Handshake(protocolVersion, serverAddress, serverPort, nextState);
            slp = new SLP(packetLength, packetID, handshake);
            handshakeProgress = 2;
        }
        if (handshakeProgress == 2) {
            if (buf.readableBytes() > 0) {
                final int statusRequest = ByteBufUtils.readVarInt(buf);
                final int pingRequest = ByteBufUtils.readVarInt(buf);
                forwardToServer(ctx, buf, playerAddress, statusRequest, pingRequest);
                handshakeProgress = 3;
            }
        } else if (handshakeProgress == 3) {
            if (node.noConnection) {
                ctx.channel().close();
                return;
            }
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
        Main.print("(1) ");
        cause.printStackTrace();
        close();
        ctx.close();
    }

    void forwardToServer(ChannelHandlerContext ctx, ByteBuf buf, InetSocketAddress playerAddress, int statusRequest, int pingRequest) {
        eventLoop = new NioEventLoopGroup();

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
                        Main.print("(2) ");
                        cause.printStackTrace();
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
                        }

                        //forward data to server
                        ByteBuf sendBuf = Unpooled.buffer();
                        ByteBufUtils.writeVarInt(sendBuf, slp.getPacketLength());
                        ByteBufUtils.writeVarInt(sendBuf, slp.getPacketID());
                        ByteBufUtils.writeVarInt(sendBuf, slp.getHandshake().getProtocolVersion());
                        ByteBufUtils.writeString(sendBuf, slp.getHandshake().getServerAddress());
                        sendBuf.writeShort(slp.getHandshake().getServerPort());
                        ByteBufUtils.writeVarInt(sendBuf, slp.getHandshake().getNextState());
                        ByteBufUtils.writeVarInt(sendBuf, statusRequest);
                        ByteBufUtils.writeVarInt(sendBuf, pingRequest);

                        sendBuf.writeBytes(buf);

                        serverChannel.writeAndFlush(sendBuf);
                        if (slp.getHandshake().getNextState() == 1) {
                            if (config.serverpingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " pinged (" + ping + "ms)");
                        } else {
                            if (config.connectingLog)
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

    private void send(ChannelHandlerContext ctx, String data) {
        ByteBuf buf = Unpooled.buffer();
        ByteBufUtils.writeVarInt(buf, 0);
        ByteBufUtils.writeString(buf, data);
        ByteBuf header = Unpooled.buffer();
        ByteBufUtils.writeVarInt(header, buf.readableBytes());
        ctx.channel().writeAndFlush(header);
        ctx.channel().writeAndFlush(buf);
        ctx.close();
    }
}

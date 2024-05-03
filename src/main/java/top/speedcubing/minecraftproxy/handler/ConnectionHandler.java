package top.speedcubing.minecraftproxy.handler;

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
import java.net.InetSocketAddress;
import java.util.Random;
import top.speedcubing.lib.minecraft.packet.HandshakePacket;
import top.speedcubing.lib.minecraft.packet.MinecraftPacket;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.lib.utils.internet.ip.CIDR;
import top.speedcubing.minecraftproxy.Main;
import top.speedcubing.minecraftproxy.config;
import top.speedcubing.minecraftproxy.server.BackendServer;
import top.speedcubing.minecraftproxy.server.Node;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    private final BackendServer server;
    private final Node node;
    private EventLoopGroup eventLoop = null;
    private Channel serverChannel;
    public State handshakeProgress = State.STATUSREQUEST;
    private int nextState;
    private int packetLength;
    private MinecraftPacket packet;

    public ConnectionHandler(Node node) {
        Main.print(hashCode()+" created");
        Random r = new Random();
        this.node = node;
        this.server = node.servers.get(r.nextInt(node.servers.size()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String player = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        //CIDR blocking
        if (handshakeProgress != State.CONNECTED) {
            InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            for (CIDR cidr : node.blockedCIDR) {
                if (cidr.contains(playerAddress.getAddress().getHostAddress())) {
                    ctx.close();
                    return;
                }
            }
        }

        ByteBuf buf = (ByteBuf) msg;

//        Main.print(player + " readable = " + buf.readableBytes());
//        if (buf.readableBytes() == 0) {
//            return;
//        }
//
//        if (handshakeProgress == State.HANDSHAKE) {
//            packetLength = ByteBufUtils.readVarInt(buf);
//            handshakeProgress = State.HANDSHAKE2;
//        }
//        if (handshakeProgress == State.HANDSHAKE2) {
//            Main.print(player + " readable = " + buf.readableBytes() + ", packetlen = " + packetLength);
//            if (buf.readableBytes() < packetLength)
//                return;
//
//            final int packetID = ByteBufUtils.readVarInt(buf);
//
//            final int protocolVersion = ByteBufUtils.readVarInt(buf);
//            final String serverAddress = ByteBufUtils.readString(buf);
//            final int serverPort = buf.readUnsignedShort();
//
//            nextState = ByteBufUtils.readVarInt(buf);
//
//            //we'll ignore status request & ping request
//
//            //login
//            if (nextState == 1) {
//                if (!node.statusRequest) {
//                    ctx.channel().close();
//                    return;
//                }
//            }
//            if (nextState == 2) {
//                if (!node.loginRequest) {
//                    if (!node.loginRequestTimeout) {
//                        ctx.channel().close();
//                    }
//                    return;
//                }
//
//                if (node.kick) {
//                    send(ctx, "\"" + node.kickMessage + "\"");
//                    return;
//                }
//            }
//
//            packet = new MinecraftPacket(packetLength, packetID, new HandshakePacket(protocolVersion, serverAddress, serverPort, nextState).toByteArray());
//            handshakeProgress = State.STATUSREQUEST;
//        }

        if (handshakeProgress == State.STATUSREQUEST) {
            if (buf.readableBytes() == 0)
                return;

            forwardToServer(ctx, buf);
            handshakeProgress = State.CONNECTED;
            return;
        }

        if (handshakeProgress == State.CONNECTED) {
            if (buf.readableBytes() == 0)
                return;
            if (serverChannel != null)
                serverChannel.writeAndFlush(buf);
            else
                buf.release();
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

    void forwardToServer(ChannelHandlerContext ctx, ByteBuf buffer) {
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
                    }
                }).connect(server.ip, server.port).addListener((ChannelFutureListener) future -> {
                    InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();

                    serverChannel = future.channel();
                    if (!future.isSuccess()) {
                        close();
                        ctx.close();
                    } else {
                        long ping = System.currentTimeMillis() - start;

                        if (server.HAProxy != null) {
                            serverChannel.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                            InetSocketAddress serverAddress = (InetSocketAddress) serverChannel.remoteAddress();
                            HAProxyMessage haProxyMessage = new HAProxyMessage(server.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, playerAddress.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), playerAddress.getPort(), serverAddress.getPort());
                            serverChannel.writeAndFlush(haProxyMessage);
                            serverChannel.pipeline().remove(HAProxyMessageEncoder.INSTANCE);
                        }

                        //forward data to server
//                        serverChannel.writeAndFlush(Unpooled.wrappedBuffer(packet.toByteArray()));
                        serverChannel.writeAndFlush(buffer);

                        if (nextState == 1) {
                            if (config.serverpingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " pinged (" + ping + "ms)");
                        } else if (nextState == 2) {
                            if (config.connectingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " connected (" + ping + "ms)");
                        }
                    }
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

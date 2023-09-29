package top.speedcubing.minecraftproxy;

import com.google.gson.stream.JsonWriter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.*;
import io.netty.util.AttributeKey;
import top.speedcubing.minecraftproxy.obj.*;

import java.io.*;
import java.net.InetSocketAddress;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    static final AttributeKey<SocketState> SOCKET_STATE = AttributeKey.valueOf("socketstate");
    static final AttributeKey<Channel> PROXY_CHANNEL = AttributeKey.valueOf("proxychannel");

    enum SocketState {
        HANDSHAKE,
        PROXY
    }

    private final Node node;
    private Channel serverToClientChannel;

    public ConnectionHandler(Node node) {
        this.node = node;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (ctx.channel().attr(SOCKET_STATE).get() == null) {
            ctx.channel().attr(SOCKET_STATE).set(SocketState.HANDSHAKE);
            ByteBuf buf = (ByteBuf) msg;
            final int packetLength = Utils.readVarInt(buf);
            final int packetID = Utils.readVarInt(buf);
            final int clientVersion = Utils.readVarInt(buf);
            final String hostname = Utils.readString(buf);
            final int port = buf.readUnsignedShort();
            final int state = Utils.readVarInt(buf);
            if (packetID == 0)
                forwardToServer(ctx, buf, packetLength, packetID, clientVersion, hostname, port, state);
        } else {
            Channel c = ctx.channel().attr(PROXY_CHANNEL).get();
            if (c != null)
                c.writeAndFlush(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (serverToClientChannel != null)
            serverToClientChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Main.print("(1) " + cause);
        ctx.close();
    }

    void forwardToServer(ChannelHandlerContext ctx, ByteBuf buf, int packetLength, int packetID, int clientVersion, String hostname, int port, int state) {
        new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<>() {
                    @Override
                    public void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx2, Object msg2) {
                                ctx.channel().writeAndFlush(msg2);
                            }
                        });
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        ctx.close();
                    }
                })
                .connect(node.remoteHost, node.remotePort).addListener((ChannelFutureListener) future -> {
                    InetSocketAddress serverAddress = (InetSocketAddress) future.channel().remoteAddress();
                    InetSocketAddress playerAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    if (!future.isSuccess()) {
                        ctx.close();
                        future.channel().close();
                        buf.release();
                    } else {
                        if (node.HAProxy != null) {
                            future.channel().pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                            future.channel().writeAndFlush(new HAProxyMessage(node.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, playerAddress.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), playerAddress.getPort(), serverAddress.getPort())).sync();
                            future.channel().pipeline().remove(HAProxyMessageEncoder.INSTANCE);
                        }
                        if (state == 1) {
                            if (Main.serverpingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " pinged");
                        } else {
                            if (Main.connectingLog)
                                Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " connected");
                            serverToClientChannel = future.channel();
                        }
                        //forward data to server
                        ByteBuf sendBuf = Unpooled.buffer();
                        Utils.writeVarInt(sendBuf, packetLength);
                        Utils.writeVarInt(sendBuf, packetID);
                        Utils.writeVarInt(sendBuf, clientVersion);
                        Utils.writeString(sendBuf, hostname);
                        Utils.writeVarShort(sendBuf, port);
                        Utils.writeVarInt(sendBuf, state);
                        while (buf.readableBytes() > 0)
                            sendBuf.writeByte(buf.readByte());
                        future.channel().writeAndFlush(sendBuf);
                        ctx.channel().attr(SOCKET_STATE).set(SocketState.PROXY);
                        ctx.channel().attr(PROXY_CHANNEL).set(future.channel());
                        buf.release();
                    }
                });
    }

    private void send(ChannelHandlerContext ctx, String data) {
        ByteBuf buf = Unpooled.buffer();
        Utils.writeVarInt(buf, 0);
        Utils.writeString(buf, data);
        ByteBuf header = Unpooled.buffer();
        Utils.writeVarInt(header, buf.readableBytes());
        ctx.channel().writeAndFlush(header);
        ctx.channel().writeAndFlush(buf);
        ctx.close();
    }
}

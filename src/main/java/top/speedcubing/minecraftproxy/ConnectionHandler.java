package top.speedcubing.minecraftproxy;

import com.google.gson.stream.JsonWriter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.*;
import io.netty.util.AttributeKey;
import top.speedcubing.lib.utils.bytes.ByteBufUtils;
import top.speedcubing.minecraftproxy.events.*;
import top.speedcubing.minecraftproxy.obj.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.NotYetConnectedException;

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
            final int packetLength = ByteBufUtils.readVarInt(buf);
            final int packetID = ByteBufUtils.readVarInt(buf);
            final int clientVersion = ByteBufUtils.readVarInt(buf);
            final String hostname = ByteBufUtils.readString(buf);
            final int port = buf.readUnsignedShort();
            final int state = ByteBufUtils.readVarInt(buf);
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
                    if (node.HAProxy != null) {
                        try {
                            future.channel().pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                            future.channel().writeAndFlush(new HAProxyMessage(node.HAProxy, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4, playerAddress.getAddress().getHostAddress(), serverAddress.getAddress().getHostAddress(), playerAddress.getPort(), serverAddress.getPort())).sync();
                            future.channel().pipeline().remove(HAProxyMessageEncoder.INSTANCE);
                        } catch (NotYetConnectedException ex) {
                            Main.print("[ERROR] NotYetConnected in HAProxy: \"" + node.name + "\"");
                        }
                    }
                    if (state == 1) {
                        ServerListPingEvent event = (ServerListPingEvent) new ServerListPingEvent(node, playerAddress).call();
                        if (Main.serverpingLog)
                            Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " pinged");
                        if (event.getServerPing() != null) {
                            handleServerPing(ctx, event.getServerPing());
                            future.channel().close();
                            return;
                        }
                    } else {
                        ServerConnectEvent event = (ServerConnectEvent) new ServerConnectEvent(node, playerAddress).call();
                        if (Main.connectingLog)
                            Main.print(playerAddress.getAddress().getHostAddress() + ":" + playerAddress.getPort() + " -> " + node + " connected");
                        if (event.getKickMessage() != null) {
                            handleKick(ctx, event.getKickMessage());
                            future.channel().close();
                            return;
                        } else
                            serverToClientChannel = future.channel();
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
                    future.channel().writeAndFlush(sendBuf);
                    ctx.channel().attr(SOCKET_STATE).set(SocketState.PROXY);
                    ctx.channel().attr(PROXY_CHANNEL).set(future.channel());
                    buf.release();
                });
    }

    private void handleServerPing(ChannelHandlerContext ctx, ServerPing serverPing) {
        StringWriter sw = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(sw);
            writer.beginObject();
            writer.name("version").beginObject();
            writer.name("name").value(serverPing.versionName);
            writer.name("protocol").value(serverPing.protocol);
            writer.endObject();

            writer.name("players").beginObject();
            writer.name("max").value(serverPing.playerMax);
            writer.name("online").value(serverPing.playerOnline);
            writer.name("sample").beginArray();
            if (serverPing.players != null && serverPing.uuids != null) {
                if (serverPing.players.length != serverPing.uuids.length)
                    throw new UnsupportedOperationException();
                for (int i = 0; i < serverPing.players.length; i++) {
                    writer.beginObject()
                            .name("name").value(serverPing.players[i])
                            .name("id").value(serverPing.uuids[i].toString()).endObject();
                }
            }
            writer.endArray();
            writer.endObject();

            writer.name("description").beginObject();
            writer.name("text").value(serverPing.text);
            writer.endObject();
            writer.endObject();

        } catch (IOException e) {
            e.printStackTrace();
        }
        send(ctx, sw.toString());
    }

    private void handleKick(ChannelHandlerContext ctx, String s) {
        send(ctx, "{\"text\":\"" + s + "\"}");
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

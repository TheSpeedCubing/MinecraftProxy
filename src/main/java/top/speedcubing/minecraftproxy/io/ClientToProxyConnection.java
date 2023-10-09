package top.speedcubing.minecraftproxy.io;

import top.speedcubing.lib.utils.IOUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class ClientToProxyConnection implements Runnable {

    public final Socket socket;
    private InputStream in;
    public OutputStream out;
    public boolean active = true;
    private final Node n;
    private ProxyToServerConnection proxyToServerConn = null;
    public ScheduledFuture<?> future;
    private boolean handshake = true;

    public ClientToProxyConnection(Socket socket, Node n) {
        this.socket = socket;
        this.n = n;
        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        } catch (IOException e) {
            active = false;
        }
    }

    byte[] buffer = new byte[1024];

    public void run() {
        while (active) {
            try {
                if (proxyToServerConn != null) {
                    if (!proxyToServerConn.active) {
                        active = false;
                        break;
                    }
                }
                if (!handshake) {
                    int read;
                    while ((read = in.read(buffer, 0, 1024)) >= 0)
                        proxyToServerConn.out.write(buffer, 0, read);
                } else {
                    DataInputStream inputStream = new DataInputStream(in);
                    int packetLength = IOUtils.readVarInt(inputStream);
                    int packetID = IOUtils.readVarInt(inputStream);
                    if (handshake) {
                        if (packetID == 0) {
                            int clientVersion = IOUtils.readVarInt(inputStream);
                            String hostname = IOUtils.readString(inputStream);
                            int port = inputStream.readUnsignedShort();
                            int state = IOUtils.readVarInt(inputStream);
                            handshake = false;
                            proxyToServerConn = new ProxyToServerConnection(this, n, packetLength, packetID, clientVersion, hostname, port, state);
                            proxyToServerConn.future = Main.ex.schedule(proxyToServerConn, 0, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            } catch (IOException e) {
                active = false;
            }
        }
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (proxyToServerConn != null) {
            proxyToServerConn.future.cancel(true);
            proxyToServerConn.active = false;
        }
    }

    public void sendMOTD() {
//        try {
//            String json = "{\"version\":{\"name\":\"PipeProxy INC.\",\"protocol\": 5},\"players\":{\"max\":1337,\"online\":0,\"sample\":[{\"name\":\"9gigsofram\",\"id\":\"4566e69f-c907-48ee-8d71-d7ba5aa00d20\"}]},\"description\":{\"text\":\"Failed to connect to requested backend server. Please contact an administartor of the proxy.\"}}";
//
//            ByteArrayOutputStream bo = new ByteArrayOutputStream();
//            DataOutputStream o = new DataOutputStream(bo);
//            IOUtils.writeVarInt(o, 0);
//            IOUtils.writeString(o, json);
//            IOUtils.writeVarInt(o, bo.size());
//            out.write(bo.toByteArray());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}

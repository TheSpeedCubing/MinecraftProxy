package top.speedcubing.minecraftproxy.io;

import top.speedcubing.lib.utils.IOUtils;
import top.speedcubing.lib.utils.internet.protocol.HAProxyProtocol;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;

public class ProxyToServerConnection implements Runnable {

    public Socket socket = null;
    private InputStream in;
    public OutputStream out;
    public boolean active = true;
    public final Node n;
    private final ClientToProxyConnection clientToProxy;
    public ScheduledFuture<?> future;
    private final int state;

    public ProxyToServerConnection(ClientToProxyConnection clientToProxy, Node n, int length, int packetID, int clientVersion, String hostname, int port, int state) {
        this.clientToProxy = clientToProxy;
        this.n = n;
        this.state = state;
        try {
            socket = new Socket(n.remoteHost, n.remotePort);
            socket.setSoTimeout(1000);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            if (n.HAProxy != null) {
                if (n.HAProxy.equals("v2")) {
                    byte[] b = HAProxyProtocol.v2(clientToProxy.socket.getInetAddress().getHostAddress(), n.remoteHost, clientToProxy.socket.getPort(), n.remotePort);
                    out.write(b);
                }
            }
            DataOutputStream outputStream = new DataOutputStream(out);
            IOUtils.writeVarInt(outputStream, length);
            IOUtils.writeVarInt(outputStream, packetID);
            IOUtils.writeVarInt(outputStream, clientVersion);
            IOUtils.writeString(outputStream, hostname);
            outputStream.writeShort(port);
            IOUtils.writeVarInt(outputStream, state);
        } catch (IOException e) {
            clientToProxy.sendMOTD();
            clientToProxy.active = false;
        }
    }

    byte[] buffer = new byte[1024];

    public void run() {
        while (active) {
            if (!clientToProxy.active) {
                active = false;
                break;
            }
            try {
                int read;
                while ((read = in.read(buffer, 0, 1024)) >= 0) {
                    clientToProxy.out.write(buffer, 0, read);
                }
                if (state == 1)
                    active = false;
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
        if (clientToProxy.future != null)
            clientToProxy.future.cancel(true);
        clientToProxy.active = false;
    }
}

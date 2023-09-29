package top.speedcubing.minecraftproxy;

import top.speedcubing.lib.eventbus.CubingEventHandler;
import top.speedcubing.minecraftproxy.events.ServerConnectEvent;
import top.speedcubing.minecraftproxy.events.ServerListPingEvent;
import top.speedcubing.minecraftproxy.obj.ServerPing;

public class EventListener {
    @CubingEventHandler
    public void a(ServerConnectEvent e) {
    }

    @CubingEventHandler
    public void a(ServerListPingEvent e) {
    }
}

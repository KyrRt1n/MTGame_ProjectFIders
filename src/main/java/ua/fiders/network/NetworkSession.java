package ua.fiders.network;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;

public class NetworkSession {

    private WebSocketServer server;
    private WebSocketClient client;
    private WebSocket hostPeer;

    private Consumer<String> messageHandler = msg -> {};
    private Runnable onConnected = () -> {};
    private Runnable onDisconnected = () -> {};

    private NetworkSession() {
    }

    public static NetworkSession host(int port) {
        NetworkSession session = new NetworkSession();
        session.server = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                if (session.hostPeer != null) {
                    conn.close(1000, "Game is full");
                    return;
                }
                session.hostPeer = conn;
                session.onConnected.run();
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                if (conn == session.hostPeer) {
                    session.hostPeer = null;
                    session.onDisconnected.run();
                }
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                session.messageHandler.accept(message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                ex.printStackTrace();
            }

            @Override
            public void onStart() {
                System.out.println("Сервер запущено на порту " + port);
            }
        };
        session.server.setReuseAddr(true);
        session.server.start();
        return session;
    }

    public static NetworkSession join(String hostIp, int port) {
        NetworkSession session = new NetworkSession();
        session.client = new WebSocketClient(URI.create("ws://" + hostIp + ":" + port)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                session.onConnected.run();
            }

            @Override
            public void onMessage(String message) {
                session.messageHandler.accept(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                session.onDisconnected.run();
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        session.client.connect();
        return session;
    }

    public void send(String message) {
        if (hostPeer != null) {
            hostPeer.send(message);
        } else if (client != null && client.isOpen()) {
            client.send(message);
        }
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void setOnConnected(Runnable onConnected) {
        this.onConnected = onConnected;
    }

    public void setOnDisconnected(Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public void close() {
        try {
            if (server != null) server.stop();
            if (client != null) client.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

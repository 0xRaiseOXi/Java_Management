package com.arbit.data.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ServerHandshake;
import com.arbit.data.Config;
import com.arbit.data.Data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


public class GateWebSocket extends WebSocketClient {
    private String serverName;
    private ConcurrentHashMap<String, Object> concurrentMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = new ConcurrentHashMap<>();
    private String message;
    private Timer pingTimer;
    private URI uri;
    
    public GateWebSocket(URI serverUri, String serverName, ConcurrentHashMap<String, Object> concurrentMap, String message) {
        super(serverUri);
        this.serverName = serverName;
        this.concurrentMap = concurrentMap;
        this.message = message;
        this.uri = serverUri;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Controller: [INFO] " + Data.getTime() + "  WebSocket " + serverName + " connected");
        Data.connections.add(this);
        send(message);

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, 5000, 5000); 
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject object = JsonParser.parseString(message).getAsJsonObject();
            String event = object.get("event").getAsString();
            if (event != null && "update".equals(event)) {
                ConcurrentHashMap<String, String> dataSymbolAdd = new ConcurrentHashMap<>();

                JsonObject dataSymbol = object.get("result").getAsJsonObject();
                String symbol = dataSymbol.get("s").getAsString();
                
                dataSymbolAdd.put("a", dataSymbol.get("a").getAsString());
                dataSymbolAdd.put("b", dataSymbol.get("b").getAsString());
                dataSymbolAdd.put("A", dataSymbol.get("A").getAsString());
                dataSymbolAdd.put("B", dataSymbol.get("B").getAsString());
    
                data.put(symbol, dataSymbolAdd);
                concurrentMap.put("Gate", data);

            } else if ("subscribe".equals(event)) {

            } else if (object.has("channel") && "spot.pong".equals(object.get("channel").getAsString())) {
                
            } 
            else {
                System.out.println("Controller: [WARNING] " + "WebSocket " + serverName + " handler not found " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Controller: [WARNING] " + Data.getTime() + "  WebSocket connection " + serverName + " closed. " + reason);

        pingTimer.cancel();
        Data.connections.remove(this);
        if (Config.autoReconnectWebSocket == 1) {
            reconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        if (ex instanceof InvalidDataException) {
            System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " error: InvalidDataException");
        } else {
            System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " error: " + ex.getMessage());
        }
    }

    public void sendPing() {
        JsonObject pingMessage = new JsonObject();
        pingMessage.addProperty("time", System.currentTimeMillis() / 1000);
        pingMessage.addProperty("channel", "spot.ping");
        send(pingMessage.toString());
    }

    public void reconnect() {
        try {
            GateWebSocket WebSocket = new GateWebSocket(uri, "Gate", concurrentMap, this.message);
            WebSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
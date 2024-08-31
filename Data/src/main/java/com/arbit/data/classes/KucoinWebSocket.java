package com.arbit.data.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.arbit.data.Config;
import com.arbit.data.Data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


public class KucoinWebSocket extends WebSocketClient {
    private String serverName = "Kucoin";
    private ConcurrentHashMap<String, Object> concurrentMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data;
    private String message;
    private Timer pingTimer;
    private URI serverURI;

    public KucoinWebSocket(URI serverUri, ConcurrentHashMap<String, Object> concurrentMap, ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data, String message) {
        super(serverUri);
        this.concurrentMap = concurrentMap;
        this.data = data;
        this.message = message;
        this.serverURI = serverUri;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Controller: [INFO] " +  Data.getTime() + "  WebSocket " + serverName  + " connected");
        Data.connections.add(this);

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, 30000, 30000); 
        send(message);
    }

    @Override
    public void onMessage(String message) {
        JsonObject object = JsonParser.parseString(message).getAsJsonObject();

        if (object.has("data")) {
            JsonObject dataObject = object.get("data").getAsJsonObject();
            String symbolName = object.get("subject").getAsString();

            ConcurrentHashMap<String, String> dataAdd = new ConcurrentHashMap<>();
            dataAdd.put("a", dataObject.get("bestAsk").getAsString());
            dataAdd.put("A", dataObject.get("bestAskSize").getAsString());
            dataAdd.put("b", dataObject.get("bestBid").getAsString());
            dataAdd.put("B", dataObject.get("bestBidSize").getAsString());

            data.put(symbolName, dataAdd);
            concurrentMap.put("Kucoin", data);

        } else if (object.has("type") && "pong".equals(object.get("type").getAsString())) {

        } else if (object.has("type") && "welcome".equals(object.get("type").getAsString())) {

        }else {
            System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " handler not found. " + message);    
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Controller: [WARNING] " + Data.getTime() + "  WebSocket " + serverName + " connection closed. " + reason);
        pingTimer.cancel();
        Data.connections.remove(this);
        if (Config.autoReconnectWebSocket == 1) {
            reconnect();
        } 
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " error: " + ex.getMessage());
    }

    public void sendPing() {
        int req_id = 10000;
        JsonObject pingMessage = new JsonObject();
        pingMessage.addProperty("type", "ping");
        pingMessage.addProperty("id", req_id);
        req_id ++;
        send(pingMessage.toString());
    }

    public void reconnect() {
        try {
            KucoinWebSocket webSocket = new KucoinWebSocket(serverURI, concurrentMap, data, message);
            webSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
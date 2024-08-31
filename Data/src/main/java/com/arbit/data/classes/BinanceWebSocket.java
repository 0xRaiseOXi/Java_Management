package com.arbit.data.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.arbit.data.Config;
import com.arbit.data.Data;
import com.google.gson.JsonObject;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonParser;

public class BinanceWebSocket extends WebSocketClient {
    private String serverName = "Binance";
    private ConcurrentHashMap<String, Object> concurrentMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data;
    private int indexServer;
    private URI urlServer;

    public BinanceWebSocket(URI serverUri, ConcurrentHashMap<String, Object> concurrentMap, ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data, int index) {
        super(serverUri);
        this.urlServer = serverUri;
        this.concurrentMap = concurrentMap;
        this.data = data;
        this.indexServer = index;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Controller: [INFO] " +  Data.getTime() + "  WebSocket " + serverName  + " " + indexServer + " connected");
        Data.connections.add(this);

    }

    @Override
    public void onMessage(String message) {
        JsonObject object = JsonParser.parseString(message).getAsJsonObject();
        if (object.has("s")) {
            String symbol = object.get("s").getAsString();
            
            ConcurrentHashMap<String, String> dataAdd = new ConcurrentHashMap<>();
            dataAdd.put("a", object.get("a").getAsString());
            dataAdd.put("A", object.get("A").getAsString());
            dataAdd.put("b", object.get("b").getAsString());
            dataAdd.put("B", object.get("B").getAsString());
            data.put(symbol, dataAdd);
            concurrentMap.put("Binance", data);
        } else if (object.has("ping")) {
            System.out.println(message);
        } 
        else {
            System.out.println("Controller: [WARNING] " + Data.getTime() + "  WebSocket " + serverName + " handler not found. " + message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Controller: [WARNING] " + Data.getTime() + "  WebSocket " + serverName + " connection closed. " + reason);
        Data.connections.remove(this);
        if (Config.autoReconnectWebSocket == 1) {
            reconnect();
        } 
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " error: " + ex.getMessage());
    }

    public void reconnect() {
        try {
            BinanceWebSocket WebSocket = new BinanceWebSocket(urlServer, concurrentMap, data, indexServer);
            WebSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
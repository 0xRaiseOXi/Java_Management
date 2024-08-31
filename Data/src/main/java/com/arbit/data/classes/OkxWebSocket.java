package com.arbit.data.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.arbit.data.Config;
import com.arbit.data.Data;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonParser;

public class OkxWebSocket extends WebSocketClient {
    private String serverName = "Okx";
    private ConcurrentHashMap<String, Object> concurrentMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data;
    private String message;
    private URI serverUrl;

    public OkxWebSocket(URI serverUri, ConcurrentHashMap<String, Object> concurrentMap, ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data, String message) {
        super(serverUri);
        this.concurrentMap = concurrentMap;
        this.data = data;
        this.message = message;
        this.serverUrl = serverUri;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Controller: [INFO] " +  Data.getTime() + "  WebSocket " + serverName  + " connected");
        Data.connections.add(this);

        send(message);
    }

    @Override
    public void onMessage(String message) {
        JsonObject object = JsonParser.parseString(message).getAsJsonObject();
        
        if (object.has("data")) {
            try {
            JsonArray dataArray = object.get("data").getAsJsonArray();
            JsonObject dataSymbol = dataArray.get(0).getAsJsonObject();

            JsonArray asksS = dataSymbol.get("asks").getAsJsonArray();
            JsonArray bidsS = dataSymbol.get("bids").getAsJsonArray();
            JsonArray ask0 = asksS.get(0).getAsJsonArray();
            JsonArray bid0 = bidsS.get(0).getAsJsonArray();

            JsonObject argSymbol = object.get("arg").getAsJsonObject();

            String symbol = argSymbol.get("instId").getAsString();
            
            ConcurrentHashMap<String, String> dataAdd = new ConcurrentHashMap<>();
            dataAdd.put("a", ask0.get(0).getAsString());
            dataAdd.put("A", ask0.get(1).getAsString());
            dataAdd.put("b", bid0.get(0).getAsString());
            dataAdd.put("B", bid0.get(1).getAsString());

            data.put(symbol, dataAdd);
            concurrentMap.put("Okx", data);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                System.out.println(message);
            }
        } else if (object.has("event")) {

        } else {
            System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " handler not found. " + message);    
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

    public void reconnect()  {
        try {
            OkxWebSocket webSocket = new OkxWebSocket(serverUrl, concurrentMap, data, message);
            webSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
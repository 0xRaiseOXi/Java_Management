package com.arbit.data.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.arbit.data.Data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

public class HTX extends WebSocketClient {
    private String serverName = "Kucoin";
    private ConcurrentHashMap<String, Object> concurrentMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data;
    private String message;

    public HTX(URI serverUri, ConcurrentHashMap<String, Object> concurrentMap, ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data, String message) {
        super(serverUri);
        this.concurrentMap = concurrentMap;
        this.data = data;
        this.message = message;
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
            JsonObject dataObject = object.get("data").getAsJsonObject();
            String symbolName = object.get("subject").getAsString();

            ConcurrentHashMap<String, String> dataAdd = new ConcurrentHashMap<>();
            dataAdd.put("a", dataObject.get("bestAsk").getAsString());
            dataAdd.put("A", dataObject.get("bestAskSize").getAsString());
            dataAdd.put("b", dataObject.get("bestBid").getAsString());
            dataAdd.put("B", dataObject.get("bestBidSize").getAsString());

            data.put(symbolName, dataAdd);
            concurrentMap.put("Kucoin", data);

        } else {

            System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " handler not found. " + message);    
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Controller: [WARNING] " + Data.getTime() + "  WebSocket " + serverName + " connection closed. " + reason + " " + code + " " + remote);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Controller: [ERROR] " + Data.getTime() + "  WebSocket " + serverName + " error: " + ex.getMessage());
    }

}
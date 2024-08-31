package com.arbit.data.classes;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.arbit.data.Config;
import com.arbit.data.Data;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;

public class BybitWebSocket extends WebSocketClient {
    private String serverName = "Bybit";
    private ConcurrentHashMap<String, Object> concurrentMap;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data;
    private int indexSocket;
    private List<String> symbols;
    private Timer pingTimer;
    private URI serverURI;

    public BybitWebSocket(URI serverUri, ConcurrentHashMap<String, Object> concurrentMap, int index, List<String> symbols, ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data) {
        super(serverUri);
        this.concurrentMap = concurrentMap;
        this.indexSocket = index;
        this.symbols = symbols;
        this.data = data;
        this.serverURI = serverUri;

    }

    public String getTime() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        String formattedDate = dateFormat.format(currentDate);
        return formattedDate;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket " + serverName + " " + indexSocket + " connected");

        JsonArray arraySymbols = new JsonArray();
        for (String symbol : symbols) {
            String symbolData = "orderbook.1." + symbol;
            arraySymbols.add(symbolData);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("op", "subscribe");
        jsonObject.add("args", arraySymbols);

        String message = jsonObject.toString();
        send(message);

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, 30000, 30000); 
        Data.connections.add(this);
    }

    @Override
    public void onMessage(String message) {
        JsonObject object = JsonParser.parseString(message).getAsJsonObject();

        if (object.has("data")) {
            JsonObject dataSymbolObject = object.get("data").getAsJsonObject();
        
            String symbol = dataSymbolObject.get("s").getAsString();
            ConcurrentHashMap<String, String> dataSymbolAdd = new ConcurrentHashMap<>();

            ConcurrentHashMap<String, String> dataP = data.get(symbol);
            
            JsonArray bid = dataSymbolObject.get("b").getAsJsonArray();
            JsonArray ask = dataSymbolObject.get("a").getAsJsonArray();
            
            if (dataP == null) {

                if (bid.isEmpty()) {
                    JsonArray askA = ask.get(0).getAsJsonArray();
                    String askS = askA.get(0).getAsString();
                    String askSV = askA.get(1).getAsString();

                    dataSymbolAdd.put("a", askS);
                    dataSymbolAdd.put("A", askSV);
                    dataSymbolAdd.put("b", "0");
                    dataSymbolAdd.put("B", "0");
                }

                else if (ask.isEmpty()) {
                    JsonArray bidA = bid.get(0).getAsJsonArray();
                    String bidS = bidA.get(0).getAsString();
                    String bidSV = bidA.get(1).getAsString();

                    dataSymbolAdd.put("b", bidS);
                    dataSymbolAdd.put("B", bidSV);
                    dataSymbolAdd.put("a", "0");
                    dataSymbolAdd.put("A", "0");

                } else {
                    JsonArray askA = ask.get(0).getAsJsonArray();
                    String askS = askA.get(0).getAsString();
                    String askSV = askA.get(1).getAsString();

                    JsonArray bidA = bid.get(0).getAsJsonArray();
                    String bidS = bidA.get(0).getAsString();
                    String bidSV = bidA.get(1).getAsString();

                    dataSymbolAdd.put("b", bidS);
                    dataSymbolAdd.put("B", bidSV);
                    dataSymbolAdd.put("a", askS);
                    dataSymbolAdd.put("A", askSV);
                }
            }
            else {

                if (bid.isEmpty()) {
                    JsonArray askA = ask.get(0).getAsJsonArray();
                    String askS = askA.get(0).getAsString();
                    String askSV = askA.get(1).getAsString();

                    dataSymbolAdd.put("a", askS);
                    dataSymbolAdd.put("A", askSV);
                    dataSymbolAdd.put("b", dataP.get("b"));
                    dataSymbolAdd.put("B", dataP.get("B"));
                }

                else if (ask.isEmpty()) {
                    JsonArray bidA = bid.get(0).getAsJsonArray();
                    String bidS = bidA.get(0).getAsString();
                    String bidSV = bidA.get(1).getAsString();

                    dataSymbolAdd.put("b", bidS);
                    dataSymbolAdd.put("B", bidSV);
                    dataSymbolAdd.put("a", dataP.get("a"));
                    dataSymbolAdd.put("A", dataP.get("A"));

                } else {
                    JsonArray askA = ask.get(0).getAsJsonArray();
                    String askS = askA.get(0).getAsString();
                    String askSV = askA.get(1).getAsString();

                    JsonArray bidA = bid.get(0).getAsJsonArray();
                    String bidS = bidA.get(0).getAsString();
                    String bidSV = bidA.get(1).getAsString();

                    dataSymbolAdd.put("b", bidS);
                    dataSymbolAdd.put("B", bidSV);
                    dataSymbolAdd.put("a", askS);
                    dataSymbolAdd.put("A", askSV);
                }
                 
            }
            data.put(symbol, dataSymbolAdd);
            concurrentMap.put("Bybit", data);
        } else if (object.has("op")) {

        } else {
            String status = object.get("success").getAsString();
            if (status == null) {
                if ("false".equals(status)) {
                    System.out.println(message);
                    System.out.println("Controller: [ERROR] " + getTime() + "  WebSocket error connect.");
                }
            } else {
                System.out.println("Controller: [WARNING] " + getTime() + "  handler not found. " + message);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Controller: [WARNING] " + getTime() + "  WebSocket " + serverName + " " + indexSocket + " connection closed.");
        pingTimer.cancel();
        Data.connections.remove(this);
        if (Config.autoReconnectWebSocket == 1) {
            reconnect();
        } 
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Controller: [ERROR] " + getTime() + "  WebSocket " + serverName + " " + indexSocket + " error: " + ex.getMessage());
    }

    public void sendPing() {
        int req_id = 10000;
        JsonObject pingMessage = new JsonObject();
        pingMessage.addProperty("op", "ping");
        pingMessage.addProperty("req_id", req_id);
        req_id ++;
        send(pingMessage.toString());
    }

    public void reconnect() {
        try {
            BybitWebSocket webSocket = new BybitWebSocket(serverURI, concurrentMap, indexSocket, symbols, data);
            webSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
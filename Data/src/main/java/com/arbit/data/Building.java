package com.arbit.data;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.java_websocket.client.WebSocketClient;

import com.arbit.arbitInEchange.BybitARBIT;
import com.arbit.data.classes.BinanceWebSocket;
import com.arbit.data.classes.BybitWebSocket;
import com.arbit.data.classes.GateWebSocket;
import com.arbit.data.classes.KucoinWebSocket;
import com.arbit.data.classes.OkxWebSocket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Building{
    private ConcurrentHashMap<String, Object> concurrentMap;
    public static ConcurrentHashMap<String, Integer> stateSymbolsExchange = new ConcurrentHashMap<>();

    private List<String> GateIOSymbols;
    private String serverBinance1 = "wss://stream.binance.com:9443/ws";
    private String serverBinance2 = "wss://stream.binance.com:9443/ws";
    private String serverBinance3 = "wss://stream.binance.com:9443/ws";
    private List<WebSocketClient> activeConnections = new ArrayList<>();
    
    private String serverBybit = "wss://stream.bybit.com/v5/public/spot";
    HashMap<Integer, List<String>> dataBybitWebSocket;

    private HashMap<Object, HashMap<String, String>> states = new HashMap<>();
    
    private String serverBinanceSymbols = "https://api.binance.com/api/v3/exchangeInfo";
    private String serverBybitSymbols = "https://api.bybit.com/v5/market/instruments-info?category=spot";
    private String serverOkxSymbols = "https://www.okx.com/api/v5/public/instruments?instType=SPOT";
    private String messageGateWebSocket;

    private String serverOkx = "wss://ws.okx.com:8443/ws/v5/public";
    private String messageOkxWebSocket;

    private String serverKucoinTOKEN = "https://api.kucoin.com/api/v1/bullet-public";
    private String serverKucoin = "wss://ws-api-spot.kucoin.com";
    private String serverKucoinToken;
    private String messageKucoinWebSocket;

    public Building(ConcurrentHashMap<String, Object> concurrentMap) {
        this.concurrentMap = concurrentMap;
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Builder launch completed");
    }

    public String getTime() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        String formattedDate = dateFormat.format(currentDate);
        return formattedDate;
    }

    public void buildExchanges(List<String> exchanges) {
        List<String> exchangesList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(exchanges.size()); 
        concurrentMap.put("connections", activeConnections);

        for (String exchange : exchanges) {
            if ("Binance".equals(exchange)) {
                executorService.execute(() -> buildBinanceWebSocket());
                exchangesList.add("Binance");
                Config.configExcnahges.put("Binance", true);
            }
            if ("Bybit".equals(exchange)) {
                executorService.execute(() -> buildBybitWebSocket());
                exchangesList.add("Bybit");
                Config.configExcnahges.put("Bybit", true);
            }
            if ("Gate".equals(exchange)) {
                executorService.execute(() -> buildGateIO());
                exchangesList.add("Gate");
                Config.configExcnahges.put("Gate", true);
            }
            if ("Okx".equals(exchange)) {
                executorService.execute(() -> buildOkx());
                exchangesList.add("Okx");
                Config.configExcnahges.put("Okx", true);
            }
            if ("Kucoin".equals(exchange)) {
                executorService.execute(() -> buildKucoin());
                exchangesList.add("Kucoin");
                Config.configExcnahges.put("Kucoin", true);
            }
        }
        concurrentMap.put("exchangeList", exchangesList);
        executorService.shutdown();
    }

    public void buildGateIO() {
        String response = AsyncRequest.getRequest(Data.GateUrlSymbols);
        GateIOSymbols = new ArrayList<>();
        JsonArray arrayJson = JsonParser.parseString(response).getAsJsonArray();
        for (int i = 0; i < arrayJson.size(); i++) {
            JsonObject jsonObject = arrayJson.get(i).getAsJsonObject();
            String name = jsonObject.get("id").getAsString();
            String status = jsonObject.get("trade_status").getAsString();

            if ("tradable".equals(status)) {
                GateIOSymbols.add(name);
            }
        }
        Config.exchangeINTEGER.put("Gate", GateIOSymbols.size());
        JsonArray jsonArray = new JsonArray();
        for (String item : GateIOSymbols) {
            jsonArray.add(item);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("time", System.currentTimeMillis() / 1000);
        jsonObject.addProperty("channel", "spot.book_ticker");
        jsonObject.addProperty("event", "subscribe");
        jsonObject.add("payload", jsonArray);

        String message = jsonObject.toString();

        this.messageGateWebSocket = message;
        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Gate complite");
        
    }
        
    public void buildBinanceWebSocket() {
        String response = AsyncRequest.getRequest(serverBinanceSymbols);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray symbols = json.getAsJsonArray("symbols");

        List<String> arraySymbols = new ArrayList<>();

        for (int i = 0; i < symbols.size(); i++) {
            JsonObject symbol = symbols.get(i).getAsJsonObject();
            String base = symbol.get("baseAsset").getAsString();
            String quote = symbol.get("quoteAsset").getAsString();
            String status = symbol.get("status").getAsString();

            if ("TRADING".equals(status)) {
                String symbolsTrading = base + quote;
                arraySymbols.add(symbolsTrading);
            }
        }
        Config.exchangeINTEGER.put("Binance", arraySymbols.size());

        int iteration = 0;
        for (String symbol : arraySymbols) {
            if (iteration < 450) {
                String symbolAdd = symbol.toLowerCase();
                String newServerBinance = serverBinance1 + String.format("/%s@bookTicker", symbolAdd);
                serverBinance1 = newServerBinance;
            }
            if (iteration >= 450 && iteration <= 900) {
                String symbolAdd = symbol.toLowerCase();
                String newServerBinance = serverBinance2 + String.format("/%s@bookTicker", symbolAdd);
                serverBinance2 = newServerBinance;
            }
            if (iteration > 900) {
                String symbolAdd = symbol.toLowerCase();
                String newServerBinance = serverBinance3 + String.format("/%s@bookTicker", symbolAdd);
                serverBinance3 = newServerBinance;
            }
            iteration ++;
        }
        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Binance complite");
    }

    public void buildBybitWebSocket() {
        String response = AsyncRequest.getRequest(serverBybitSymbols);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject resultKey = json.get("result").getAsJsonObject();
        JsonArray listKey = resultKey.get("list").getAsJsonArray();

        List<String> arraySymbols = new ArrayList<>();

        for (int i = 0; i < listKey.size(); i++) {
            JsonObject symbol = listKey.get(i).getAsJsonObject();
            String base = symbol.get("baseCoin").getAsString();
            String quote = symbol.get("quoteCoin").getAsString();
            String status = symbol.get("status").getAsString();

            if ("Trading".equals(status)) {
                String symbolsTrading = base + quote;
                arraySymbols.add(symbolsTrading);
            }
        }
        Config.exchangeINTEGER.put("Bybit", arraySymbols.size());
        int indexSocket = 0;
        int flag = 10;
        int iteration = 1;
        List<String> symbolsBuild = new ArrayList<>();
        HashMap<Integer, List<String>> dataBybitWebSocketBuild = new HashMap<>();

        for (String symbol : arraySymbols) {
            symbolsBuild.add(symbol);
            if (iteration == flag){
                flag += 10;
                indexSocket += 1;
                List<String> symbolsAdd = new ArrayList<>();
                
                for (String symbolArray: symbolsBuild) {
                    symbolsAdd.add(symbolArray);
                }
                symbolsBuild.clear();
                dataBybitWebSocketBuild.put(indexSocket, symbolsAdd);
            }
            iteration ++;
        }
        
        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Bybit complite");
        dataBybitWebSocket = dataBybitWebSocketBuild;
    }

    public void buildOkx() {
        String response = AsyncRequest.getRequest(serverOkxSymbols);
        List<String> OkxSymbols = new ArrayList<>();
        JsonObject dataJson = JsonParser.parseString(response).getAsJsonObject();
        JsonArray arrayJson = dataJson.get("data").getAsJsonArray();

        for (int i = 0; i < arrayJson.size(); i++) {
            JsonObject jsonObject = arrayJson.get(i).getAsJsonObject();
            String name = jsonObject.get("instId").getAsString();
            String status = jsonObject.get("state").getAsString();

            if ("live".equals(status)) {
                OkxSymbols.add(name);
            }
        }

        Config.exchangeINTEGER.put("Okx", OkxSymbols.size());
        JsonArray jsonArray = new JsonArray();
        for (String item : OkxSymbols) {
            JsonObject objectSymbol = new JsonObject();
            objectSymbol.addProperty("channel", "bbo-tbt");
            objectSymbol.addProperty("instId", item);
            jsonArray.add(objectSymbol);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("op", "subscribe");
        jsonObject.add("args", jsonArray);

        String message = jsonObject.toString();
        this.messageOkxWebSocket = message;

        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Okx complite");
    }

    public void buildKucoin() {
        String response = AsyncRequest.postRequest(serverKucoinTOKEN, "");
        JsonObject responseTOKEN = JsonParser.parseString(response).getAsJsonObject();
        JsonObject dataTOKEN = responseTOKEN.get("data").getAsJsonObject();
        String TOKEN = dataTOKEN.get("token").getAsString();

        serverKucoinToken = serverKucoin + "/?token=" + TOKEN;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "92748273");
        jsonObject.addProperty("type", "subscribe");
        jsonObject.addProperty("topic", "/market/ticker:all");
        jsonObject.addProperty("response", "false");

        String message = jsonObject.toString();
        this.messageKucoinWebSocket = message;
        Config.exchangeINTEGER.put("Kucoin", 0);

        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Kucoin complite");
    }

    public void conectExchanges() {
        List<String> exchangesList = (List<String>) concurrentMap.get("exchangeList");

        for (String exchange : exchangesList) {
            if ("Binance".equals(exchange)) {
                connectBinance();
            }
            if ("Bybit".equals(exchange)) {
                connectBybit();
            }
            if ("Gate".equals(exchange)) {
                connectGate();
            }
            if ("Okx".equals(exchange)) {
                connectOkx();
            }
            if ("Kucoin".equals(exchange)) {
                connectKucoin();
            }
        }
    }

    public void connectBinance() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = new ConcurrentHashMap<>();
        
        int indexBinanceSocket = 1;
        BinanceWebSocket webSocket = new BinanceWebSocket(URI.create(serverBinance1), concurrentMap, data, indexBinanceSocket);
        webSocket.connect();
        HashMap<String, String> stateBinance = new HashMap<>();
        stateBinance.put("Binance", "connect");
        states.put(webSocket, stateBinance);
        indexBinanceSocket ++;

        BinanceWebSocket webSocket2 = new BinanceWebSocket(URI.create(serverBinance2), concurrentMap, data, indexBinanceSocket);
        webSocket2.connect();
        HashMap<String, String> stateBinance2 = new HashMap<>();
        stateBinance2.put("Binance2", "connect");
        states.put(webSocket2, stateBinance2);
        indexBinanceSocket ++;

        BinanceWebSocket webSocket3 = new BinanceWebSocket(URI.create(serverBinance3), concurrentMap, data, indexBinanceSocket);
        webSocket3.connect();
        HashMap<String, String> stateBinance3 = new HashMap<>();
        stateBinance3.put("Binance3", "connect");
        states.put(webSocket3, stateBinance3);

        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Binance connect");
        
    }
    public void connectGate() {
        try {
            URI url = new URI(Data.urlGateToWebSocket);
            GateWebSocket WebSocket = new GateWebSocket(url, "Gate", concurrentMap, this.messageGateWebSocket);
            WebSocket.connect();
            System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Gate connect");
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public void connectBybit() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = new ConcurrentHashMap<>();
        for (int i = 1; i <= dataBybitWebSocket.size(); i ++){
            List<String> dataSymbols = dataBybitWebSocket.get(i);
            BybitWebSocket webSocket = new BybitWebSocket(URI.create(serverBybit), concurrentMap, i, dataSymbols, data);
            webSocket.connect();
        }
        System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Bybit connect");
    }

    public void connectOkx() {
        try {
            ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = new ConcurrentHashMap<>();
            OkxWebSocket webSocket = new OkxWebSocket(URI.create(serverOkx), concurrentMap, data, this.messageOkxWebSocket);
            webSocket.connect();
            
            System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Okx connect");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectKucoin() {
        try {
            ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = new ConcurrentHashMap<>();
            KucoinWebSocket webSocket = new KucoinWebSocket(URI.create(serverKucoinToken), concurrentMap, data, this.messageKucoinWebSocket);
            webSocket.connect();
            
            System.out.println("Controller: [INFO] " + getTime() + "  WebSocket Okx connect");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void disconnectAllExchange() {
        int activeThreadCount = Thread.activeCount();
        System.out.println("Количество активных потоков: " + activeThreadCount);
        System.out.println("Controller: [INFO] " + getTime() + " Active Threads: " + activeThreadCount);
        for (WebSocketClient connect : Data.connections) {
            connect.close();
        }
        try {
        Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Controller: [INFO] " + getTime() + " WebSocket connections disconnect.");
        int activeThreadCountafter = Thread.activeCount();

        System.out.println("Controller: [INFO] " + getTime() + " Active Threads: " + activeThreadCountafter);
    }

}
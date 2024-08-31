package com.arbit.arbitInEchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.arbit.data.AsyncRequest;
import com.arbit.data.Data;
import com.arbit.server.WebSocketHadler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BybitARBIT implements Runnable{
    private List<String> symbols = new ArrayList<>();
    private HashMap<String, HashMap<String, String>> settings = new HashMap<>();
    private List<String> pairs = new ArrayList<>();
    private List<List<String>> dataITERATION= new ArrayList<>();
    private ConcurrentHashMap<String, Object> concurrentMap;

    public BybitARBIT (ConcurrentHashMap<String, Object> concurrentMap) {
        this.concurrentMap = concurrentMap;
    }
    
    public void loadSymbols() {
        String symbols = AsyncRequest.getRequest("https://api.bybit.com/v5/market/instruments-info?category=spot");
        JsonObject json = JsonParser.parseString(symbols).getAsJsonObject().get("result").getAsJsonObject();
        JsonArray symbolsArray = json.get("list").getAsJsonArray();

        for (int i = 1; i < symbolsArray.size(); i++) {
            JsonObject symbol = symbolsArray.get(i).getAsJsonObject();
            String symbolName = symbol.get("symbol").getAsString();
            String base = symbol.get("baseCoin").getAsString();
            String quote = symbol.get("quoteCoin").getAsString();

            String pair = base + '/' + quote;
            
            HashMap<String, String> symbolData = new HashMap<>();
            symbolData.put("b", base);
            symbolData.put("q", quote);

            this.settings.put(symbolName, symbolData);
            this.symbols.add(symbolName);
            this.pairs.add(pair);
        }
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Arbit Bybit symbols load complite.");
    }

    public void buildSymbolsToIteration() {
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Arbit Bybit build start");

        HashMap<String, String> data;
        HashMap<String, String> data2;
        String dataSymbol;
        String dataSymbol2;

        for (String symbol : this.symbols) {
            for (String symbol2 : this.symbols) {
                if (!symbol.equals(symbol2)) {

                    data = this.settings.get(symbol);
                    dataSymbol = data.get("b");

                    data2 = this.settings.get(symbol2);
                    dataSymbol2 = data2.get("b");

                    if (dataSymbol.equals(dataSymbol2)) {
                        List<String> dataAdd = new ArrayList<>();
                        dataAdd.add(symbol);
                        dataAdd.add(symbol2);

                        this.dataITERATION.add(dataAdd);
                    }
                }
            }
           
        }
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Arbit Bybit build symbols complite.");
    }
    
    public void runIteartion() {
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Arbit Bybit start");
        HashMap<String, String> dataSymbol = new HashMap<>();
        HashMap<String, String> dataSymbol2 = new HashMap<>();
        HashMap<String, String> dataSymbol3 = new HashMap<>();
        HashMap<String, String> dataSymbol4= new HashMap<>();

        double symbolPrice;
        double symbol2Price;
        double symbol3Price;
        double symbol4Price;

        String symbol;
        String symbol2;
        String symbol3 = null;
        String symbol4 = null;

        double a;
        double b;
        
        while (!Thread.currentThread().isInterrupted()) {
            for (int i = 0; i < dataITERATION.size(); i++) {
                List<String> dataSymbols = dataITERATION.get(i);

                symbol = dataSymbols.get(0);
                symbol2 = dataSymbols.get(1);

                dataSymbol = settings.get(symbol);
                dataSymbol2 = settings.get(symbol2);
                
                if (dataSymbol.get("q").equals("USDT")) {
                    symbolPrice = Double.parseDouble(getData(symbol, "ask"));
                    a = (50 / symbolPrice) * (1 - 0.1 / 100);

                } else {
                    symbol3 = dataSymbol.get("q") + "USDT";
                    symbol3Price = Double.parseDouble(getData(symbol3, "ask"));

                    if (symbol3Price != 0) {
                        symbolPrice = Double.parseDouble(getData(symbol, "ask")) * symbol3Price;
                    } else {
                        symbol3 = "USDT" + dataSymbol.get("q");
                        symbol3Price = Double.parseDouble(getData(symbol3, "bid"));

                        if (symbol3Price != 0) {
                            symbolPrice = Double.parseDouble(getData(symbol, "ask")) / symbol3Price;
                        } else {
                            continue;
                        }
                    }
                    a = (40 / symbolPrice) * (1 - 0.2 / 100);
                }

                if (dataSymbol2.get("q").equals("USDT")) {
                    symbol2Price = Double.parseDouble(getData(symbol2, "bid"));
                    b = (a * symbol2Price) * (1 - 0.1 / 100);
                } else {
                    symbol4 = dataSymbol2.get("q") + "USDT";
                    symbol4Price = Double.parseDouble(getData(symbol4, "bid"));

                    if (symbol4Price != 0) {
                        symbol2Price = Double.parseDouble(getData(symbol2, "bid")) * symbol4Price;
                    } else {
                        symbol4 = "USDT" + dataSymbol2.get("q");
                        symbol4Price = Double.parseDouble(getData(symbol4, "ask"));
                        if (symbol4Price != 0) {
                            symbol2Price = Double.parseDouble(getData(symbol2, "bid")) / symbol4Price;
                        } else {
                            continue;
                        }
                    }
                    b = (a * symbol2Price) * (1 - 0.2 / 100);
                }
                if (b > 50) {
                    System.out.println("> 50 " + symbol + " " + symbol2 + " " + b);
                    String message;

                    if (symbol3 != null) {
                        if (symbol4 != null) {
                            message = "{'type': 'a', 's': '" + symbol + "', 's2': '" + symbol2 + "', 's3': '" + symbol3 + "', 's4': '" + symbol4 + "', 'p': '" + b + "'";
                        }
                        else {
                            message = "{'type': 'a', 's': '" + symbol + "', 's2': '" + symbol2 + "', 's3': '" + symbol3 + "', 'p': '" + b + "'";
                        }
                    } else {
                        message = "{'type': 'a', 's': '" + symbol + "', 's2': '" + symbol2 + "', 's4': '" + symbol4 + "', 'p': '" + b + "'";
                    }
                    WebSocketHadler.sendMessageToChannel("bybitARBIT", message);
                }
            }
        }
    }

    public String getData(String symbol, String type) {
        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> dataExchange = (ConcurrentHashMap<String, ConcurrentHashMap<String, String>>) concurrentMap.get("Bybit");
        ConcurrentHashMap<String, String> dataSymbol = dataExchange.get(symbol);

        if (dataSymbol != null) {
            if ("ask".equals(type)) {
                String ask = dataSymbol.get("a");
                return ask;
            } else if ("bid".equals(type)) {
                String bid = dataSymbol.get("b");
                return bid;
            }

        } else {   
            return "0";
        }
        return "0";
    } 

    @Override
    public void run() {
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Arbit Bybit start");
        loadSymbols();
        buildSymbolsToIteration();
        runIteartion();
    }
}

package com.arbit.data;

import java.util.concurrent.ConcurrentHashMap;

public class Config {
    public static int autoReconnectWebSocket = 1;

    public static ConcurrentHashMap<String, Boolean> configExcnahges = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Integer> exchangeINTEGER = new ConcurrentHashMap<>();


    public static Thread threadBYBIT;
}

package com.arbit;

import java.util.concurrent.ConcurrentHashMap;
import com.arbit.data.Data;
import com.arbit.server.Server;

public class Main {
    private static ConcurrentHashMap<String, Object> concurrentMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Thread Server = new Thread(new Server(concurrentMap));
        Server.start();
        System.out.println("PulsarCORE: [INFO] " + Data.getTime() + "  Server start compite.");

    }

}
package com.arbit.arbitInEchange;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.arbit.data.Data;

public class Runner{
    private ConcurrentHashMap<String, Object> concurrentMap;
    private HashMap<String, Thread> dataThreads = new HashMap<>();

    public Runner(ConcurrentHashMap<String, Object> concurrentMap) {
        this.concurrentMap = concurrentMap;
        System.out.println("Controller: [INFO] " + Data.getTime() + "  Runner launch completed");
    }

    public String bybitArbit(String state) {
        if ("start".equals(state)) {
            if (!dataThreads.containsKey("Bybit")) {
                Thread threadBybit = new Thread(new BybitARBIT(concurrentMap));
                threadBybit.start();
                System.out.println("Runner: [INFO] " + Data.getTime() + "  Thread Bybit started.");
                dataThreads.put("Bybit", threadBybit);
                return "{'state': 'OK', 'message': 'Thread started', 'code': 1}";
            } else {
                System.out.println("Runner: [INFO] " + Data.getTime() + "  Thread Bybit is already running.");
                return "{'state': 'error', 'message': 'Bybit is already running', 'code': 0}";
            }

        } else if ("stop".equals(state)) {
            Thread bybitThread = dataThreads.get("Bybit");

            if (bybitThread != null) {
                bybitThread.interrupt();
                dataThreads.remove("Bybit");
                System.out.println("Runner: [INFO] " + Data.getTime() + "  Thread Bybit stoped.");
                return "{'state': 'OK', 'message': 'Thread stopted', 'code': 1}";
            } else {
                System.out.println("Runner: [INFO] " + Data.getTime() + "  Thread Bybit not started.");
                return "{'state': 'error', 'message': 'Thread Bybit not started', 'code': 0}";
            }
        }
        return null;
    }
}

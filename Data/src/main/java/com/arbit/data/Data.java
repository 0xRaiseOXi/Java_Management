package com.arbit.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.java_websocket.client.WebSocketClient;

public class Data {
    public static String messageToGateIO;
    public static String urlGateToWebSocket = "wss://api.gateio.ws/ws/v4/";
    public static String GateUrlSymbols = "https://api.gateio.ws/api/v4/spot/currency_pairs";

    public static List<WebSocketClient> connections = new ArrayList<>();

    public static String getTime() {
        Date currentDate = new Date();

        // Создаем объект SimpleDateFormat для форматирования даты и времени
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

        // Преобразуем текущее время в строку с заданным форматом
        String formattedDate = dateFormat.format(currentDate);
        return formattedDate;
    }
}

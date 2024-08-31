package com.arbit.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.arbit.arbitInEchange.Runner;
import com.arbit.data.Building;
import com.arbit.data.Data;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;


public class WebSocketHadler extends SimpleChannelInboundHandler<WebSocketFrame>{
    private static Map<Channel, Set<String>> subscribedChannels = new HashMap<>();
    private ConcurrentHashMap<String, Object> concurrentMap;
    private Building build;
    private Runner runner;

    public WebSocketHadler(ConcurrentHashMap<String, Object> concurrentMap) {
        this.concurrentMap = concurrentMap;
        build = new Building(concurrentMap);
        runner = new Runner(concurrentMap);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        subscribedChannels.put(ctx.channel(), new HashSet<>());
        ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"status\":\"OK\"}"));
    }

    private void subscribeToChannel(Channel channel, String channelName) {
        System.out.println("Подписываю на канал" + channel + channelName);
        Set<String> userChannels = subscribedChannels.get(channel);
        if (userChannels != null) {
            userChannels.add(channelName);
        }
    }

    public static void sendMessageToChannel(String channelName, String message) {
        for (Map.Entry<Channel, Set<String>> entry : subscribedChannels.entrySet()) {
            Channel subscriber = entry.getKey();
            Set<String> userChannels = entry.getValue();

            if (userChannels.contains(channelName)) {
                // subscriber.writeAndFlush(new TextWebSocketFrame("[" + channelName + "] " + message));
                subscriber.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
            String request = textFrame.text();
            
            try {
                JsonObject json = JsonParser.parseString(request).getAsJsonObject();
                // System.out.println(json);

                String type = json.get("type").getAsString();
                if ("compile".equals(type)){
                    JsonArray dataArray = json.get("exchanges").getAsJsonArray();
                    List<String> exchanges = new ArrayList<>();

                    for (int i = 0; i < dataArray.size(); i++) {
                        String object = dataArray.get(i).getAsString();
                        exchanges.add(object);
                    }
                    build.buildExchanges(exchanges);
                }

                if ("start".equals(type)){
                    build.conectExchanges();
                }
                else if ("get".equals(type)){
                    // ConcurrentHashMap<String, ConcurrentHashMap<String, String>> data = concurrentMap.get("Bybit");
                    JsonObject object = JsonParser.parseString(concurrentMap.toString()).getAsJsonObject();

                    // JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
                    String dataMessage = object.toString();
                    ctx.writeAndFlush(new TextWebSocketFrame(dataMessage));
                }
                else if ("disconnect".equals(type)) {
                    build.disconnectAllExchange();
                    
                } else if ("statusINTEGER".equals(type)) {
                    // JsonObject object = new JsonObject();

                } else if ("arbit".equals(type)) {
                    String paramArbit = json.get("params").getAsString();
                    String state = runner.bybitArbit(paramArbit);
                    ctx.writeAndFlush(new TextWebSocketFrame(state));

                } else {
                    System.out.println("Controller: [WARNING] " + Data.getTime() + "  handler to request not found. " + request);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (request.startsWith("/subscribe ")) {
                String channelName = request.substring("/subscribe ".length()).trim();
                subscribeToChannel(ctx.channel(), channelName);
                ctx.writeAndFlush(new TextWebSocketFrame("{\"status\":\"OK\"}"));

            } else if (request.startsWith("/send ")) {
                String[] parts = request.split(" ", 3);
                if (parts.length == 3) {
                    String channelName = parts[1];
                    String message = parts[2];
                    sendMessageToChannel(channelName, message);
                    ctx.writeAndFlush(new TextWebSocketFrame("{\"status\":\"OK\"}"));
                }
            } else {
                ctx.writeAndFlush(new TextWebSocketFrame("Error. Обработчик не найден"));
            }
        } else {
        }
    }

}

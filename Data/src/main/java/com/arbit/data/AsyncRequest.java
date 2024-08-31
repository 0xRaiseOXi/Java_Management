package com.arbit.data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


public class AsyncRequest {
    
    public static CompletableFuture<String> asyncGetRequest(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
    
    public static String getRequest(String url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);

            String responseBody = EntityUtils.toString(response.getEntity());

            response.close();
            httpClient.close();
            
            return responseBody;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String postRequest(String url, String requestBody) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);

        // Устанавливаем тело POST-запроса (если оно есть)
        if (requestBody != null) {
            StringEntity entity = new StringEntity(requestBody, "UTF-8");
            httpPost.setEntity(entity);
        }

        try {
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity());

            response.close();
            httpClient.close();

            return responseBody;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // public static void main(String[] args) {
    //     String url = "https://api.binance.com/api/v3/exchangeInfo";
        
    //     long start = System.currentTimeMillis();

    //     CompletableFuture<String> responceFuture = asyncGetRequest(url);

    //     responceFuture.thenAccept(responseBody -> {
    //         System.out.println("Ответ: ");
    //         // System.out.println(responseBody);
    //     });
    //     long end = System.currentTimeMillis() - start;

    //     CompletableFuture.allOf(responceFuture).join();
    //     System.out.println("Time " + end + "mc");

    // }

}
